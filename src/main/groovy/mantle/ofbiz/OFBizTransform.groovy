/*
 * This software is in the public domain under CC0 1.0 Universal plus a
 * Grant of Patent License.
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package mantle.ofbiz

import groovy.transform.CompileStatic
import org.moqui.Moqui
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFacade
import org.moqui.entity.EntityFind
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.etl.SimpleEtl
import org.moqui.etl.SimpleEtl.EntryTransform
import org.moqui.etl.SimpleEtl.SimpleEntry
import org.moqui.etl.SimpleEtl.Transformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp

import static mantle.ofbiz.OFBizFieldMap.map

@CompileStatic
class OFBizTransform {
    static Logger logger = LoggerFactory.getLogger(OFBizTransform.class)

    static List<List<String>> loadOrderParallel = [
            ['NoteData', 'Party', 'ContactMech', 'Product', 'Lot', 'OrderHeader'],
            ['PartyNote', 'Person', 'PartyGroup', 'PartyRole', 'UserLogin',
                    'PartyContactMechPurpose', 'PostalAddress', 'TelecomNumber', 'PaymentMethod',
                    'InventoryItem', 'PhysicalInventory',
                    'Shipment', 'ShipmentBoxType',
                    'Invoice', 'FinAccount', 'GlJournal'], // NOTE skipping: 'ProductPrice',
            ['PartyClassification', 'PartyRelationship', 'CreditCard',
                    'OrderItemShipGroup', 'OrderHeaderNote', 'CostComponent',
                    'ShipmentItem', 'ShipmentPackage', 'ShipmentRouteSegment',
                    'InvoiceContactMech', 'InvoiceRole', 'InvoiceItem',
                    'FinAccountTrans'],
            ['OrderRole', 'Payment'],
            ['OrderItem'],
            ['OrderAdjustment', 'OrderContactMech', 'OrderItemShipGrpInvRes', 'OrderPaymentPreference',
                    'ItemIssuance', 'ShipmentReceipt', 'ShipmentPackageContent', 'ShipmentPackageRouteSeg', 'OrderShipment'],
            ['PaymentApplication', 'PaymentGatewayResponse', 'PaymentCreditType',
                    'InventoryItemDetail', 'OrderItemBilling', 'OrderAdjustmentBilling',
                    'AcctgTrans'],
            ['AcctgTransEntry', 'PaymentCredit']
    ]

    // NOTE: for really large imports there may be memory constraint issues and this would need to a be a disk based cache
    static Map<String, Map<String, Object>> mappingCaches = new HashMap<>()
    static Map<String, Object> getMappingCache(String name) {
        Map<String, Object> cache = mappingCaches.get(name)
        if (cache == null) { cache = new HashMap<>(); mappingCaches.put(name, cache) }
        return cache
    }
    static void clearMappingCaches() { mappingCaches.clear() }

    static SimpleEtl.TransformConfiguration conf = new SimpleEtl.TransformConfiguration()
    static {
        // load NoteData to temporary table to join with Party/Order/etc note records
        conf.addTransformer("NoteData", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String noteInfo = val.noteInfo
            if (!noteInfo || "NONE".equals(noteInfo)) { et.loadCurrent(false); return }
            if (noteInfo.length() > 4000) val.noteInfo = noteInfo.substring(0,4000)
            et.addEntry(new SimpleEntry("mantle.ofbiz.TemporaryNote", val))
        }})

        /* ========== AcctgTrans ========== */

        conf.addTransformer("GlJournal", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.ledger.transaction.GlJournal", [glJournalId:val.glJournalId,
                    glJournalName:val.glJournalName, organizationPartyId:val.organizationPartyId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("AcctgTrans", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // skip some invalid dates
            String transactionDate = val.transactionDate
            String postedDate = val.postedDate
            if (transactionDate?.startsWith("3") || postedDate?.startsWith("3")) {
                Map<String, Object> skipCache = getMappingCache("AcctgTransSkip")
                skipCache.put((String) val.acctgTransId, true)
                et.loadCurrent(false)
                return
            }
            // warn about future dates
            Timestamp nowStamp = new Timestamp(System.currentTimeMillis())
            if (transactionDate) {
                try { if (Timestamp.valueOf(transactionDate) > nowStamp) logger.warn("AcctgTrans ${val.acctgTransId} has future transactionDate ${transactionDate}") }
                catch (Exception e) { logger.warn("Error checking AcctgTrans timestamps", e) }
            }
            if (postedDate) {
                try { if (Timestamp.valueOf(postedDate) > nowStamp) logger.warn("AcctgTrans ${val.acctgTransId} has future postedDate ${postedDate}") }
                catch (Exception e) { logger.warn("Error checking AcctgTrans timestamps", e) }
            }
            // if tx type is AttInvoiceAdjust and there is no invoiceId but there is a paymentId then change it to AttPaymentAdjust
            String acctgTransTypeEnumId = map('acctgTransTypeId', (String) val.acctgTransTypeId)
            if ("AttInvoiceAdjust".equals(acctgTransTypeEnumId) && !val.invoiceId && val.paymentId)
                acctgTransTypeEnumId = "AttPaymentAdjust"

            et.addEntry(new SimpleEntry("mantle.ledger.transaction.AcctgTrans", [acctgTransId:val.acctgTransId,
                    acctgTransTypeEnumId:acctgTransTypeEnumId, otherPartyId:val.partyId,
                    organizationPartyId:'Company', // if there are multiple internal orgs with transactions remove this and uncomment code in AcctgTransEntry
                    amountUomId:'USD', // change this for other currencies, if there are multiple currencies need to get from AcctgTransEntry like organizationPartyId
                    description:val.description, transactionDate:transactionDate, isPosted:val.isPosted, postedDate:postedDate,
                    scheduledPostingDate:val.scheduledPostingDate, voucherRef:val.voucherRef, voucherDate:val.voucherDate,
                    assetId:val.inventoryItemId, physicalInventoryId:val.physicalInventoryId, invoiceId:val.invoiceId,
                    paymentId:val.paymentId, finAccountTransId:val.finAccountTransId, shipmentId:val.shipmentId,
                    assetReceiptId:val.receiptId, theirAcctgTransId:val.theirAcctgTransId, glJournalId:val.glJournalId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // handle? not really used: glFiscalTypeId, groupStatusId
            // NOTE: skipping because FixedAsset not yet transformed: fixedAssetId
            // NOTE: skipping because WorkEffort not yet transformed: workEffortId
            // NOTE: no organizationPartyId on AcctgTrans in OFBiz, get it from AcctgTransEntry record
        }})
        conf.addTransformer("AcctgTransEntry", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String acctgTransId = val.acctgTransId
            Map<String, Object> skipCache = getMappingCache("AcctgTransSkip")
            if (skipCache.get(acctgTransId)) { et.loadCurrent(false); return }
            // make sure AcctgTrans exists
            EntityValue acctgTrans = Moqui.executionContext.entity.find("mantle.ledger.transaction.AcctgTrans")
                    .condition("acctgTransId", acctgTransId).one()
            if (acctgTrans == null) {
                logger.warn("AcctgTransEntry ${val.acctgTransEntrySeqId} references AcctgTrans ${val.acctgTransId} that does not exist, skipping")
                skipCache.put(acctgTransId, true)
                et.loadCurrent(false)
                return
            }
            // make sure shipment exists, shouldn't happen but an issue in one production data set, comment this if not needed:
            String productId = val.productId
            if (productId && Moqui.executionContext.entity.find("mantle.product.Product").condition("productId", productId).one() == null) {
                logger.warn("For AcctgTrans ${val.acctgTransId} Entry ${val.acctgTransEntrySeqId} Product ${productId} does not exist, not setting")
                productId = null
            }
            // always using 'Company' for organizationPartyId as an optimization, use this when there are multiple internal orgs with transactions:
            // EntityValue acctgTrans = Moqui.executionContext.entity.find("mantle.ledger.transaction.AcctgTrans").condition("acctgTransId", val.acctgTransId).one()
            // if (!acctgTrans.organizationPartyId) { acctgTrans.organizationPartyId = val.organizationPartyId; acctgTrans.update() }
            et.addEntry(new SimpleEntry("mantle.ledger.transaction.AcctgTransEntry", [acctgTransId:acctgTransId,
                    acctgTransEntrySeqId:val.acctgTransEntrySeqId, description:val.description, voucherRef:val.voucherRef,
                    productId:productId, externalProductId:val.theirProductId, assetId:val.inventoryItemId,
                    glAccountTypeEnumId:map('glAccountTypeId', (String) val.glAccountTypeId), dueDate:val.dueDate,
                    glAccountId:map('glAccountId', (String) val.glAccountId),
                    amount:val.amount, debitCreditFlag:val.debitCreditFlag, originalCurrencyAmount:val.origAmount,
                    isSummary:val.isSummary, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // not used in mantle, not really in OFBiz either: acctgTransEntryTypeId
            // could map if all SettlementTerm records are in place, not generally used: settlementTermId
        }})

        /* ========== ContactMech ========== */

        conf.addTransformer("ContactMech", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String contactMechTypeEnumId = OFBizFieldMap.get('contactMechTypeId', (String) val.contactMechTypeId)
            if (!contactMechTypeEnumId) { logger.info("Skipping ContactMech ${val.contactMechId} of type ${val.contactMechTypeId}"); et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.party.contact.ContactMech", [contactMechId:val.contactMechId,
                    contactMechTypeEnumId:contactMechTypeEnumId, infoString:val.infoString, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PartyContactMechPurpose", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // NOTE: for allowSolicitation, extension, comments, etc would have to combine with PartyContactMech
            et.addEntry(new SimpleEntry("mantle.party.contact.PartyContactMech", [partyId:val.partyId, contactMechId:val.contactMechId,
                    contactMechPurposeId:map('contactMechPurposeTypeId', (String) val.contactMechPurposeTypeId),
                    fromDate:val.fromDate, thruDate:val.thruDate, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PostalAddress", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            Map<String, Object> entryMap = new HashMap<>(val) // most fields match name, rest will be ignored
            // not importing Geo records, mapping to Moqui ones, may import or map in the future
            entryMap.remove('cityGeoId'); entryMap.remove('countyGeoId'); entryMap.remove('municipalityGeoId'); entryMap.remove('postalCodeGeoId')
            entryMap.remove('geoPointId') // may import GeoPoint in future
            String stateProvinceGeoId = val.stateProvinceGeoId
            // TODO: do any cleanup on address1, etc?
            if (stateProvinceGeoId && stateProvinceGeoId.length() == 2) {
                // NOTE: this only handles USA and CAN states/provinces which had no prefix in OFBiz
                if (stateProvinceGeoId in ['AB','BC','MB','NB','NL','NS','NT','NU','ON','PE','QC','SK','YT']) stateProvinceGeoId = 'CAN_' + stateProvinceGeoId
                else stateProvinceGeoId = 'USA_' + stateProvinceGeoId
            } else if (stateProvinceGeoId) {
                EntityValue spGeo = Moqui.executionContext.entity.find("moqui.basic.Geo").condition("geoId", stateProvinceGeoId).one()
                if (spGeo == null) {
                    logger.info("State/province Geo ${stateProvinceGeoId} not found, not setting on address")
                    stateProvinceGeoId = null
                }
            }
            String postalCode = val.postalCode
            if (postalCode) postalCode = postalCode.replaceAll(/\W/, "").toUpperCase()
            String postalCodeExt = val.postalCodeExt
            if (postalCodeExt) postalCodeExt = postalCodeExt.replaceAll(/\W/, "").toUpperCase()
            et.addEntry(new SimpleEntry("mantle.party.contact.PostalAddress", entryMap + ([
                    stateProvinceGeoId:stateProvinceGeoId, postalCode:postalCode, postalCodeExt:postalCodeExt,
                    unitNumber:((String) val.houseNumber + (val.houseNumberExt ? '-' + (String) val.houseNumberExt : '')),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)] as Map<String, Object>)))
        }})
        conf.addTransformer("TelecomNumber", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String countryCode = val.countryCode
            String areaCode = val.areaCode
            String contactNumber = val.contactNumber
            if (countryCode) countryCode = countryCode.replaceAll(/\D/, "")
            if (areaCode) areaCode = areaCode.replaceAll(/\D/, "")

            int countryLength = countryCode != null ? countryCode.length() : 0
            // this isn't the best definition of an international country code, meant to handle bad data where the USA areaCode is in the countryCode field
            boolean isIntl = countryLength > 0 && (countryCode.length() < 3 || countryCode.startsWith("0")) && !"1".equals(countryCode) && !"11".equals(countryCode)
            if (isIntl) {
                logger.info("Found international number, not cleaning: [${countryCode}] [${areaCode}] [${contactNumber}]")
            } else {
                // TODO: if you are not using USA (or similar 3,3,4) numbers only change or disable this!
                String fullNum = ""
                if (countryCode && !"1".equals(countryCode)) { fullNum += countryCode; countryCode = "1" }
                if (areaCode) fullNum += areaCode
                if (contactNumber) contactNumber = contactNumber.replaceAll(/\D/, "")
                if (contactNumber) fullNum += contactNumber
                if (fullNum) {
                    int fullNumLen = fullNum.length()
                    if (fullNumLen <= 7) {
                        areaCode = null
                        contactNumber = fullNumLen > 3 ? fullNum.substring(0,3) + '-' + fullNum.substring(3) : fullNum
                    } else {
                        areaCode = fullNum.substring(0,3)
                        contactNumber = fullNum.substring(3,6) + '-' + fullNum.substring(6)
                    }
                    if (!countryCode) countryCode = "1"
                }
            }
            et.addEntry(new SimpleEntry("mantle.party.contact.TelecomNumber", [contactMechId:val.contactMechId,
                    countryCode:countryCode, areaCode:areaCode, contactNumber:contactNumber,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== FinAccount ========== */

        conf.addTransformer("FinAccount", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.account.financial.FinancialAccount", [finAccountId:val.finAccountId,
                    finAccountTypeId:map('finAccountTypeId', (String) val.finAccountTypeId),
                    statusId:map('finAccountStatusId', (String) val.statusId), finAccountName:val.finAccountName,
                    finAccountCode:val.finAccountCode, finAccountPin:val.finAccountPin, currencyUomId:val.currencyUomId,
                    organizationPartyId:val.organizationPartyId, ownerPartyId:val.ownerPartyId, fromDate:val.fromDate,
                    thruDate:val.thruDate, isRefundable:val.isRefundable, replenishPaymentId:val.replenishPaymentId,
                    replenishLevel:val.replenishLevel, actualBalance:val.actualBalance, availableBalance:val.availableBalance,
                    postToGlAccountId:map('glAccountId', (String) val.postToGlAccountId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp)?.take(23)]))
        }})
        conf.addTransformer("FinAccountTrans", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            if (val.paymentId) {
                // create a dummy Payment since that and FinancialAccountTrans refer to each other so no clean load order, but this avoids having to create dummy records lower down
                et.addEntry(new SimpleEntry("mantle.account.payment.Payment", [paymentId:val.paymentId]))
            }
            et.addEntry(new SimpleEntry("mantle.account.financial.FinancialAccountTrans", [finAccountTransId:val.finAccountTransId,
                    finAccountId:val.finAccountId, fromPartyId:val.partyId, transactionDate:val.transactionDate, entryDate:val.entryDate,
                    finAccountTransTypeEnumId:map('finAccountTransTypeId', (String) val.finAccountTransTypeId),
                    amount:val.amount, paymentId:val.paymentId, orderId:val.orderId, orderItemSeqId:val.orderItemSeqId,
                    comments:val.comments, lastUpdatedStamp:((String) val.lastUpdatedTxStamp)?.take(23)]))
            // NOTE: putting partyId in fromPartyId, could maybe be mapped to fromPartyId OR toPartyId depending on...?
            // NOTE: reasonEnumId not yet mapped, not used as much in OFBiz as in Mantle
        }})

        /* ========== Inventory ========== */

        conf.addTransformer("Lot", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.product.asset.Lot", [lotId:val.lotId, creationDate:val.creationDate, lotNumber:val.lotId,
                    manufacturedDate:val.creationDate, quantity:val.quantity, expirationDate:((String) val.expirationDate)?.take(10),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp)?.take(23)]))
        }})
        conf.addTransformer("InventoryItem", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue product = Moqui.executionContext.entity.find("mantle.product.Product").condition("productId", val.productId).one()
            if (product == null) { logger.warn("Skipping InventoryItem ${val.inventoryItemId}, product referenced ${val.productId} does not exist"); et.loadCurrent(false); return }
            // some example key cleanup
            String locationSeqId = val.locationSeqId
            if (locationSeqId) { int spaceIdx = locationSeqId.indexOf(" "); if (spaceIdx > 0) locationSeqId = locationSeqId.substring(0, spaceIdx) }
            if (val.lotId && val.partyId) {
                // InventoryItem.partyId is used as supplier/manufacturer party, so set on Lot if there is one; should be an update as Lots already loaded
                EntityValue lot = Moqui.executionContext.entity.find("mantle.product.asset.Lot").condition("lotId", val.lotId).one()
                if (lot == null) {
                    et.addEntry(new SimpleEntry("mantle.product.asset.Lot", [lotId:val.lotId, lotNumber:val.lotId, mfgPartyId:val.partyId]))
                } else if (!lot.mfgPartyId) {
                    lot.put("mfgPartyId", val.partyId)
                    lot.update()
                }
            }
            et.addEntry(new SimpleEntry("mantle.product.asset.Asset", [assetId:val.inventoryItemId, productId:val.productId,
                    assetTypeEnumId:(product.assetTypeEnumId ?: 'AstTpInventory'), classEnumId:product.assetClassEnumId,
                    hasQuantity:(val.inventoryItemTypeId == 'SERIALIZED_INV_ITEM' ? 'N' : 'Y'), ownerPartyId:val.ownerPartyId,
                    facilityId:val.facilityId, locationSeqId:locationSeqId, statusId:(map('inventoryStatusId', (String) val.statusId) ?: 'AstAvailable'),
                    receivedDate:val.datetimeReceived, manufacturedDate:val.datetimeManufactured, expectedEndOfLife:((String) val.expireDate)?.take(10),
                    lotId:val.lotId, comments:val.comments, quantityOnHandTotal:val.quantityOnHandTotal,
                    availableToPromiseTotal:val.availableToPromiseTotal, serialNumber:val.serialNumber,
                    softIdentifier:val.softIdentifier, activationNumber:val.activationNumber, activationValidThru:val.activationValidThru,
                    acquireCost:val.unitCost, acquireCostUomId:val.currencyUomId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: facilityId passed through as-is, assuming Facility already exists (currently no transform for it)
            // NOTE: locationSeqId passed through cleaned up, assuming FacilityLocation already exists (currently no transform for it)
            // NOTE: containerId not yet handled, need to also transform Container before this
        }})
        conf.addTransformer("InventoryItemDetail", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // needs: Asset, Shipment, Return, AssetIssuance, AssetReceipt, PhysicalInventory
            String productId = Moqui.executionContext.entity.find("mantle.product.asset.Asset").condition("assetId", val.inventoryItemId).one()?.productId
            // make sure shipment exists, shouldn't happen but an issue in one production data set, comment this if not needed:
            String shipmentId = val.shipmentId
            if (shipmentId && Moqui.executionContext.entity.find("mantle.shipment.Shipment").condition("shipmentId", shipmentId).one() == null) {
                logger.warn("For InventoryItem ${val.inventoryItemId} Detail ${val.inventoryItemDetailSeqId} Shipment ${shipmentId} does not exist, not setting")
                shipmentId = null
            }
            et.addEntry(new SimpleEntry("mantle.product.asset.AssetDetail", [
                    assetDetailId:((String) val.inventoryItemId) + '_' + ((String) val.inventoryItemDetailSeqId),
                    assetId:val.inventoryItemId, productId:productId, effectiveDate:val.effectiveDate, unitCost:val.unitCost,
                    quantityOnHandDiff:val.quantityOnHandDiff, availableToPromiseDiff:val.availableToPromiseDiff,
                    shipmentId:shipmentId, assetIssuanceId:val.itemIssuanceId, assetReceiptId:val.receiptId,
                    physicalInventoryId:val.physicalInventoryId, description:val.description,
                    varianceReasonEnumId:map('varianceReasonEnumId', (String) val.reasonEnumId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: workEffortId not yet handled, need to also transform WorkEffort for this
            // NOTE: Return not yet handled: returnId:val.returnId, returnItemSeqId:val.returnItemSeqId,
        }})

        conf.addTransformer("PhysicalInventory", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.product.asset.PhysicalInventory", [physicalInventoryId:val.physicalInventoryId,
                    physicalInventoryDate:val.physicalInventoryDate, partyId:val.partyId, comments:val.generalComments,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        conf.addTransformer("OrderItemShipGrpInvRes", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // needs: Asset, OrderItem
            String productId = Moqui.executionContext.entity.find("mantle.product.asset.Asset").condition("assetId", val.inventoryItemId).one()?.productId
            // TODO: populate quantityNotAvailable and quantityNotIssued
            et.addEntry(new SimpleEntry("mantle.product.issuance.AssetReservation", [assetReservationId:UUID.randomUUID().toString(),
                    assetId:val.inventoryItemId, orderId:val.orderId, orderItemSeqId:val.orderItemSeqId, productId:productId,
                    quantity:val.quantity, quantityNotAvailable:val.quantityNotAvailable, reservedDate:val.reservedDatetime,
                    originalPromisedDate:val.promisedDatetime, currentPromisedDate:val.currentPromisedDate,
                    priority:(val.priority == 'Y' ? 10 : 0), sequenceNum:val.sequenceId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: not mapping reserveOrderEnumId
        }})
        conf.addTransformer("ItemIssuance", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // needs: Asset, OrderItem, Shipment
            String productId = Moqui.executionContext.entity.find("mantle.product.asset.Asset").condition("assetId", val.inventoryItemId).one()?.productId
            et.addEntry(new SimpleEntry("mantle.product.issuance.AssetIssuance", [assetIssuanceId:val.itemIssuanceId,
                    assetId:val.inventoryItemId, orderId:val.orderId, orderItemSeqId:val.orderItemSeqId, shipmentId:val.shipmentId,
                    productId:productId, issuedDate:val.issuedDateTime, quantity:val.quantity, quantityCancelled:val.cancelQuantity,
                    issuedByUserId:val.issuedByUserLoginId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("ShipmentReceipt", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // needs: Asset, OrderItem, Shipment, ReturnItem
            et.addEntry(new SimpleEntry("mantle.product.receipt.AssetReceipt", [assetReceiptId:val.receiptId,
                    assetId:val.inventoryItemId, productId:val.productId, orderId:val.orderId, orderItemSeqId:val.orderItemSeqId,
                    shipmentId:val.shipmentId, shipmentPackageSeqId:val.shipmentPackageSeqId,
                    rejectionReasonEnumId:map('rejectionId', (String) val.rejectionId),
                    receivedByUserId:val.receivedByUserLoginId, receivedDate:val.datetimeReceived, itemDescription:val.itemDescription,
                    quantityAccepted:val.quantityAccepted, quantityRejected:val.quantityRejected,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: skipping because returns not yet handled: returnId:val.returnId, returnItemSeqId:val.returnItemSeqId
        }})
        conf.addTransformer("CostComponent", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // NOTE: supports only costComponentTypeId=EST_STD_GEN_COST,GEN_COST setting Asset.acquireCost in matching date range
            // needs: Asset
            et.loadCurrent(false) // we'll never load a value, just updating records
            if (val.costComponentTypeId != 'EST_STD_GEN_COST' && val.costComponentTypeId != 'GEN_COST') return
            if (!val.cost) return

            Timestamp fromDate = Moqui.executionContext.l10n.parseTimestamp((String) val.fromDate, null)
            Timestamp thruDate = Moqui.executionContext.l10n.parseTimestamp((String) val.thruDate, null)

            EntityFacade entity = Moqui.executionContext.entity
            EntityFind assetFind = entity.find("mantle.product.asset.Asset")
                    .condition("productId", val.productId)
            if (fromDate != (Object) null || thruDate != (Object) null) {
                List andList = []
                if (fromDate != (Object) null) andList.add(entity.conditionFactory.makeCondition("receivedDate", EntityCondition.GREATER_THAN_EQUAL_TO, fromDate))
                if (thruDate != (Object) null) andList.add(entity.conditionFactory.makeCondition("receivedDate", EntityCondition.LESS_THAN_EQUAL_TO, thruDate))
                // allow null receivedDate for all CostComponent records, will effectively set it to value of last matching product in file, generally the most recent as OFBiz sorts exports by last updated stamp
                assetFind.condition(entity.conditionFactory.makeCondition([entity.conditionFactory.makeCondition("receivedDate", EntityCondition.LESS_THAN_EQUAL_TO, thruDate),
                        entity.conditionFactory.makeCondition(andList)], EntityCondition.JoinOperator.OR))
            }
            EntityList assetList = assetFind.list()
            int nullReceived = 0
            for (EntityValue asset in assetList) {
                asset.set("acquireCost", new BigDecimal((String) val.cost))
                asset.update()
                if (asset.receivedDate == null) nullReceived++
            }
            logger.info("Updated ${assetList.size()} Asset records (${nullReceived} null rec date) with CostComponent cost ${val.cost} for product ${val.productId} from ${fromDate} thru ${thruDate}")
        }})


        /* ========== Invoice ========== */

        conf.addTransformer("Invoice", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String statusMapType = (((String) val.invoiceTypeId) in ['SALES_INVOICE', 'SALES_INV_TEMPLATE', 'PURC_RTN_INVOICE']) ?
                    'invoiceOutgoingStatusId' : 'invoiceIncomingStatusId'
            et.addEntry(new SimpleEntry("mantle.account.invoice.Invoice", [invoiceId:val.invoiceId,
                    invoiceTypeEnumId:map('invoiceTypeId', (String) val.invoiceTypeId),
                    statusId:map(statusMapType, (String) val.statusId),
                    fromPartyId:val.partyIdFrom, toPartyId:val.partyId, billingAccountId:val.billingAccountId,
                    invoiceDate:val.invoiceDate, dueDate:val.dueDate, paidDate:val.paidDate, invoiceMessage:val.invoiceMessage,
                    referenceNumber:val.referenceNumber, description:val.description, currencyUomId:val.currencyUomId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("InvoiceContactMech", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.account.invoice.InvoiceContactMech", [invoiceId:val.invoiceId,
                    contactMechId:val.contactMechId, contactMechPurposeId:map('contactMechPurposeTypeId', (String) val.contactMechPurposeTypeId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("InvoiceRole", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            if ("_NA_".equals(val.roleTypeId)) { et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.account.invoice.InvoiceParty", [invoiceId:val.invoiceId, partyId:val.partyId,
                    roleTypeId:map('roleTypeId', (String) val.roleTypeId), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("InvoiceItem", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue invoice = Moqui.executionContext.entity.find("mantle.account.invoice.Invoice").condition("invoiceId", val.invoiceId).one()
            et.addEntry(new SimpleEntry("mantle.account.invoice.InvoiceItem", [invoiceId:val.invoiceId,
                    invoiceItemSeqId:val.invoiceItemSeqId, itemTypeEnumId:map('invoiceItemTypeId', (String) val.invoiceItemTypeId),
                    assetId:val.inventoryItemId, productId:val.productId, taxableFlag:val.taxableFlag, quantity:(val.quantity ?: '1'),
                    quantityUomId:val.uomId, amount:val.amount, description:val.description, itemDate:invoice.invoiceDate,
                    overrideGlAccountId:map('glAccountId', (String) val.overrideGlAccountId),
                    salesOpportunityId:val.salesOpportunityId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: could look up taxAuthorityId by taxAuthGeoId and taxAuthPartyId
            // NOTE: skipping, not generally needed and avoids fk error: parentInvoiceId:val.parentInvoiceId, parentInvoiceItemSeqId:val.parentInvoiceItemSeqId
        }})

        /* ========== Order ========== */

        conf.addTransformer("OrderHeader", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.order.OrderHeader", [orderId:val.orderId, orderName:val.orderName, orderRevision:1,
                    salesChannelEnumId:map('salesChannelEnumId', (String) val.salesChannelEnumId),
                    statusId:map('orderStatusId', (String) val.statusId),
                    currencyUomId:val.currencyUom, entryDate:val.entryDate, placedDate:val.orderDate, externalId:val.externalId,
                    terminalId:val.terminalId, parentOrderId:val.firstAttemptOrderId, remainingSubTotal:val.remainingSubTotal,
                    grandTotal:val.grandTotal, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: orderTypeId not needed, implied by customer and vendor parties; originFacilityId not needed, has facilityId on OrderPart
            // NOTE: not mapping visitId, productStoreId, billingAccountId, syncStatusId (though could be at some point)
        }})
        conf.addTransformer("OrderItemShipGroup", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue orderHeader = Moqui.executionContext.entity.find("mantle.order.OrderHeader").condition("orderId", val.orderId).one()
            def partTotal = orderHeader?.grandTotal
            et.addEntry(new SimpleEntry("mantle.order.OrderPart", [orderId:val.orderId, statusId:orderHeader.statusId,
                    orderPartSeqId:((String) val.shipGroupSeqId).padLeft(5, '0'),
                    shipmentMethodEnumId:map('shipmentMethodTypeId', (String) val.shipmentMethodTypeId),
                    vendorPartyId:val.vendorPartyId/*usually null, expected from OrderRole*/, carrierPartyId:val.carrierPartyId,
                    facilityId:val.facilityId, postalContactMechId:val.contactMechId, telecomContactMechId:val.telecomContactMechId,
                    trackingNumber:val.trackingNumber, shippingInstructions:val.shippingInstructions, maySplit:val.maySplit,
                    giftMessage:val.giftMessage, isGift:val.isGift, shipAfterDate:val.shipAfterDate, shipBeforeDate:val.shipByDate,
                    estimatedShipDate:val.estimatedShipDate, estimatedDeliveryDate:val.estimatedDeliveryDate, partTotal:partTotal,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("OrderRole", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup
            if ("_NA_".equals(val.roleTypeId)) { et.loadCurrent(false); return }
            // BILL_TO_CUSTOMER, END_USER_CUSTOMER, PLACING_CUSTOMER, SHIP_TO_CUSTOMER; BILL_FROM_VENDOR, SHIP_FROM_VENDOR, SUPPLIER_AGENT
            // EntityList orderPartList = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId).list()
            // for (EntityValue orderPart in orderPartList) {
                // String orderPartSeqId = orderPart.orderPartSeqId
                String orderPartSeqId = "00001" // optimized for only single part orders
                if ("BILL_TO_CUSTOMER".equals(val.roleTypeId)) {
                    et.addEntry(new SimpleEntry("mantle.order.OrderPart", [orderId:val.orderId, orderPartSeqId:orderPartSeqId, customerPartyId:val.partyId]))
                } else if ("BILL_FROM_VENDOR".equals(val.roleTypeId)) {
                    et.addEntry(new SimpleEntry("mantle.order.OrderPart", [orderId:val.orderId, orderPartSeqId:orderPartSeqId, vendorPartyId:val.partyId]))
                }
                et.addEntry(new SimpleEntry("mantle.order.OrderPartParty", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                        partyId:val.partyId, roleTypeId:map('roleTypeId', (String) val.roleTypeId),
                        lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // }
        }})
        conf.addTransformer("OrderContactMech", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup
            // EntityList orderPartList = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId).list()
            // for (EntityValue orderPart in orderPartList) {
                // String orderPartSeqId = orderPart.orderPartSeqId
                String orderPartSeqId = "00001" // optimized for only single part orders
                et.addEntry(new SimpleEntry("mantle.order.OrderPartContactMech", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                        contactMechPurposeId:map('contactMechPurposeTypeId', (String) val.contactMechPurposeTypeId),
                        contactMechId:val.contactMechId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // }
        }})
        conf.addTransformer("OrderItem", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup
            String orderPartSeqId = "00001" // optimized for only single part orders
            EntityValue orderPart = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId).
                    condition("orderPartSeqId", orderPartSeqId).one()
            if (orderPart == null) {
                logger.info("Found OrderItem in order ${val.orderId} that has no OrderPart, creating dummy record")
                orderPart = Moqui.executionContext.entity.makeValue("mantle.order.OrderPart")
                        .set("orderId", val.orderId).set("orderPartSeqId", orderPartSeqId).create()
            }
            // if OrderItem has correspondingPoId set on OrderPart.otherPartyOrderId
            if (val.correspondingPoId) {
                if (orderPart.otherPartyOrderId && !((String) orderPart.otherPartyOrderId).contains((String) val.correspondingPoId)) {
                    orderPart.otherPartyOrderId = ((String) orderPart.otherPartyOrderId) + ', ' + val.correspondingPoId
                } else { orderPart.otherPartyOrderId = val.correspondingPoId }
                orderPart.update()
            }
            et.addEntry(new SimpleEntry("mantle.order.OrderItem", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    orderItemSeqId:val.orderItemSeqId, productId:val.productId, otherPartyProductId:val.supplierProductId,
                    itemTypeEnumId:map('orderItemTypeId', (String) val.orderItemTypeId),
                    productFeatureId:val.productFeatureId, productCategoryId:val.productCategoryId, comments:val.comments,
                    itemDescription:val.itemDescription, quantity:val.quantity, quantityCancelled:val.cancelQuantity,
                    selectedAmount:val.selectedAmount, requiredByDate:val.shipBeforeDate,
                    unitAmount:val.unitPrice, unitListPrice:val.unitListPrice, isModifiedPrice:val.isModifiedPrice,
                    externalItemSeqId:val.externalId, fromAssetId:val.fromInventoryItemId, isPromo:val.isPromo,
                    subscriptionId:val.subscriptionId, salesOpportunityId:val.salesOpportunityId,
                    overrideGlAccountId:map('glAccountId', (String) val.overrideGlAccountId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        // NOTE: nothing for OrderItemShipGroupAssoc, doing single part orders only
        conf.addTransformer("OrderAdjustment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup
            String orderPartSeqId = "00001" // optimized for only single part orders
            et.addEntry(new SimpleEntry("mantle.order.OrderItem", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    orderItemSeqId:val.orderAdjustmentId, itemTypeEnumId:map('orderAdjustmentTypeId', (String) val.orderAdjustmentTypeId),
                    parentItemSeqId:(val.orderItemSeqId ?: val.originalAdjustmentId), comments:val.comments, itemDescription:val.description,
                    quantity:1.0, unitAmount:val.amount, amountAlreadyIncluded:val.amountAlreadyIncluded,
                    productFeatureId:val.productFeatureId, sourceReferenceId:val.sourceReferenceId, sourcePercentage:val.sourcePercentage,
                    customerReferenceId:val.customerReferenceId, exemptAmount:val.exemptAmount,
                    isPromo:(val.orderAdjustmentTypeId == 'PROMOTION_ADJUSTMENT' ? 'Y' : 'N'), isModifiedPrice:val.isManual,
                    overrideGlAccountId:map('glAccountId', (String) val.overrideGlAccountId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: doesn't map to TaxAuthority.taxAuthorityId, could look up by taxAuthGeoId, taxAuthPartyId
        }})
        conf.addTransformer("OrderPaymentPreference", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup, and for from/to parties after OrderRole
            // must be after Payment so PaymentIdByOppId cache is populated
            Map<String, Object> paymentIdByOppId = getMappingCache("PaymentIdByOppId")
            String paymentId = paymentIdByOppId.get((String) val.orderPaymentPreferenceId)

            String orderPartSeqId = "00001" // optimized for only single part orders
            if (paymentId) {
                EntityValue payment = Moqui.executionContext.entity.find("mantle.account.payment.Payment").condition("paymentId", paymentId).one()
                if (!payment.orderId) {
                    payment.orderId = val.orderId
                    payment.orderPartSeqId = orderPartSeqId
                }
                if (!payment.paymentAuthCode) payment.paymentAuthCode = val.manualAuthCode
                if (!payment.paymentRefNum) payment.paymentRefNum = val.manualRefNum
                payment.update()
                et.loadCurrent(false)
            } else {
                // do anything with OPP records in certain statuses like PAYMENT_RECEIVED, PAYMENT_SETTLED, PMNT_RECEIVED, PMNT_SENT, PMNT_CONFIRMED?
                // OPP records in those statuses SHOULD have a Payment record to map to but not all do
                if (((String) val.statusId) in ['PAYMENT_RECEIVED', 'PAYMENT_SETTLED', 'PMNT_RECEIVED', 'PMNT_SENT', 'PMNT_CONFIRMED'])
                    logger.warn("Found OrderPaymentPreference ${val.orderPaymentPreferenceId} for order ${val.orderId} in ${val.statusId} with no corresponding Payment record")
                EntityValue orderPart = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId)
                        .condition("orderPartSeqId", orderPartSeqId).one()
                et.addEntry(new SimpleEntry("mantle.account.payment.Payment", [paymentId:'OPP' + ((String) val.orderPaymentPreferenceId),
                        paymentTypeEnumId:'PtOrderPref', orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                        fromPartyId:orderPart?.customerPartyId, toPartyId:orderPart?.vendorPartyId,
                        paymentInstrumentEnumId:map('paymentInstrumentEnumId', (String) val.paymentMethodTypeId),
                        paymentMethodId:val.paymentMethodId, finAccountId:val.finAccountId, presentFlag:val.presentFlag,
                        swipedFlag:val.swipedFlag, amount:val.maxAmount, processAttempt:val.processAttempt, needsNsfRetry:val.needsNsfRetry,
                        paymentAuthCode:val.manualAuthCode, paymentRefNum:val.manualRefNum, statusId:'PmntCancelled',
                        effectiveDate:((String) val.lastUpdatedTxStamp).take(23), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
                // NOTE setting all cancelled so we don't get unapplied seemingly valid Payment records: statusId:map('paymentStatusId', (String) val.statusId)
            }
        }})
        conf.addTransformer("OrderItemBilling", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            //
            String orderPartSeqId = "00001" // optimized for only single part orders
            EntityValue orderPart = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId).
                    condition("orderPartSeqId", orderPartSeqId).one()
            if (orderPart?.otherPartyOrderId) {
                EntityValue invoice = Moqui.executionContext.entity.find("mantle.account.invoice.Invoice").condition("invoiceId", val.invoiceId).one()
                if (invoice != null) {
                    if (invoice.referenceNumber && !((String) invoice.referenceNumber).contains((String) orderPart.otherPartyOrderId)) {
                        invoice.referenceNumber = ((String) invoice.referenceNumber) + ', ' + orderPart.otherPartyOrderId
                    } else { invoice.referenceNumber = orderPart.otherPartyOrderId }
                    invoice.update()
                }
            }

            // after OrderItem, InvoiceItem, AssetReceipt, AssetIssuance
            et.addEntry(new SimpleEntry("mantle.order.OrderItemBilling", [orderItemBillingId:UUID.randomUUID().toString(),
                    orderId:val.orderId, orderItemSeqId:val.orderItemSeqId, invoiceId:val.invoiceId, invoiceItemSeqId:val.invoiceItemSeqId,
                    assetIssuanceId:val.itemIssuanceId, assetReceiptId:val.shipmentReceiptId, quantity:val.quantity, amount:val.amount,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("OrderAdjustmentBilling", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String orderId = Moqui.executionContext.entity.find("mantle.order.OrderItem").condition("orderItemSeqId", val.orderAdjustmentId).one()?.orderId
            et.addEntry(new SimpleEntry("mantle.order.OrderItemBilling", [orderItemBillingId:UUID.randomUUID().toString(),
                    orderId:orderId, orderItemSeqId:val.orderAdjustmentId, invoiceId:val.invoiceId, invoiceItemSeqId:val.invoiceItemSeqId,
                    quantity:1.0, amount:val.amount, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("OrderHeaderNote", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue temporaryNote = Moqui.executionContext.entity.find("mantle.ofbiz.TemporaryNote").condition("noteId", val.noteId).one()
            if (temporaryNote == null) { et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.order.OrderNote", [orderId:val.orderId, internalNote:val.internalNote,
                    noteDate:temporaryNote.noteDateTime, noteText:temporaryNote.noteInfo,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== Party ========== */

        conf.addTransformer("Party", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.party.Party", [partyId:val.partyId, partyTypeEnumId:map('partyTypeId', (String) val.partyTypeId),
                externalId:val.externalId, disabled:("PARTY_DISABLED".equals(val.statusId) ? 'Y' : 'N'), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PartyGroup", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.party.Organization", [partyId:val.partyId, organizationName:((String) val.groupName)?.trim(),
                officeSiteName:(!val.officeSiteName || ((String) val.officeSiteName).toLowerCase() == ((String) val.groupName).toLowerCase() ? '' : val.officeSiteName),
                annualRevenue:val.annualRevenue, numEmployees:val.numEmployees, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PartyNote", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue temporaryNote = Moqui.executionContext.entity.find("mantle.ofbiz.TemporaryNote").condition("noteId", val.noteId).one()
            if (temporaryNote == null) { et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.party.PartyNote", [partyId:val.partyId, internalNote:'Y',
                    noteDate:temporaryNote.noteDateTime, noteText:temporaryNote.noteInfo,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PartyRelationship", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // TODO: should from/to be reversed? relationship types and approach varies somewhat so some, but not all, may need reversing
            et.addEntry(new SimpleEntry("mantle.party.PartyRelationship", [partyRelationshipId:UUID.randomUUID().toString(),
                    relationshipTypeEnumId:map('partyRelationshipTypeId', (String) val.partyRelationshipTypeId),
                    fromPartyId:val.partyIdFrom, fromRoleTypeId:map('roleTypeId', (String) val.roleTypeIdFrom),
                    toPartyId:val.partyIdTo, toRoleTypeId:map('roleTypeId', (String) val.roleTypeIdTo),
                    fromDate:val.fromDate, thruDate:val.thruDate, comments:val.comments, relationshipName:val.relationshipName,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: doesn't map statusId, maybe do in future if ever used
        }})
        conf.addTransformer("PartyRole", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String roleTypeId = val.roleTypeId
            // handle _NA_ role which is a placeholder in OFBiz and never needed (should be eliminated) in Moqui
            // NOTE: may want to remove the INTERNAL_ORGANIZATIO check here, avoid annoyance with demo data/etc of internal orgs, assume all desired setup already
            if ("_NA_".equals(roleTypeId) || "INTERNAL_ORGANIZATIO".equals(roleTypeId)) { et.loadCurrent(false); return }
            String rtMapped = OFBizFieldMap.get('roleTypeId', roleTypeId)
            if (rtMapped == null) {
                if (Moqui.executionContext.entity.find("mantle.party.RoleType").condition("roleTypeId", roleTypeId).useCache(true).one() == null) {
                    logger.info("Adding RoleType ${roleTypeId}")
                    et.addEntry(new SimpleEntry("mantle.party.RoleType", [roleTypeId:roleTypeId,
                            description:roleTypeId.split("_").collect({ ((String) it).toLowerCase().capitalize() }).join(" ")] as Map<String, Object>))
                }
                rtMapped = roleTypeId
            }
            et.addEntry(new SimpleEntry("mantle.party.PartyRole", [partyId:val.partyId, roleTypeId:rtMapped, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            if (rtMapped in ['CustomerBillTo', 'CustomerEndUser', 'CustomerPlacing', 'CustomerShipTo'])
                et.addEntry(new SimpleEntry("mantle.party.PartyRole", [partyId:val.partyId, roleTypeId:'Customer', lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("Person", new Transformer() { void transform(EntryTransform et) {
            Map<String, Object> val = et.entry.etlValues
            String firstName = ((String) val.firstName)?.trim()?.capitalize()
            String middleName = ((String) val.middleName)?.trim()?.capitalize()
            String lastName = ((String) val.lastName)?.trim()?.capitalize()
            Map<String, Object> entryMap = new HashMap<>(val) // most fields match name, rest will be ignored
            entryMap.putAll([firstName:firstName, middleName:middleName, lastName:lastName, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)])
            et.addEntry(new SimpleEntry("mantle.party.Person", entryMap))
            // NOTE could add PartyIdentification records for socialSecurityNumber, passportNumber/passportExpireDate
        }})
        conf.addTransformer("UserLogin", new Transformer() { void transform(EntryTransform et) {
            Map<String, Object> val = et.entry.etlValues
            String currentPassword = (String) val.currentPassword
            String passwordHashType = null
            String passwordSalt = null
            String passwordBase64 = 'N'
            if (currentPassword != null) {
                // for password syntax and other nuances see org.ofbiz.base.crypto.HashCrypt
                int hashTypeEnd = -1
                int saltEnd = -1
                if (currentPassword.startsWith('$')) {
                    hashTypeEnd = currentPassword.indexOf('$', 1)
                    saltEnd = currentPassword.indexOf('$', hashTypeEnd + 1)
                } else if (currentPassword.startsWith('{')) {
                    hashTypeEnd = currentPassword.indexOf('}', 1)
                }
                if (hashTypeEnd > 0) {
                    passwordHashType = currentPassword.substring(1, hashTypeEnd)
                    if (saltEnd > 0) {
                        // if saltEnd is > 0 we have an OFBiz posix style password so is Base64 encoded
                        passwordBase64 = 'Y'
                        passwordSalt = currentPassword.substring(hashTypeEnd + 1, saltEnd)
                        currentPassword = currentPassword.substring(saltEnd + 1)
                    } else {
                        currentPassword = currentPassword.substring(hashTypeEnd + 1)
                    }
                }
            }
            // create the UserAccount record directly here so we can add Party/Person/etc records immediately
            et.loadCurrent(false)
            Moqui.executionContext.service.sync().name("create#moqui.security.UserAccount")
                .parameters([userId:val.userLoginId, username:val.userLoginId, partyId:val.partyId, passwordHint:val.passwordHint,
                    requirePasswordChange:val.requirePasswordChange, disabled:(val.enabled == 'N' ? 'Y' : 'N'),
                    disabledDateTime:val.disabledDateTime, successiveFailedLogins:val.successiveFailedLogins,
                    currencyUomId:val.lastCurrencyUom, locale:val.lastLocale, timeZone:val.lastTimeZone,
                    currentPassword:currentPassword, passwordSalt:passwordSalt, passwordHashType:passwordHashType, passwordBase64:passwordBase64,
                    passwordSetDate:((String) val.lastUpdatedTxStamp).take(23), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)])
                .call()
            if (!val.partyId) {
                Moqui.executionContext.service.sync().name("mantle.party.PartyServices.create#UserAccountPerson")
                        .parameter("userId", val.userLoginId).call()
            }
        }})

        /* ========== Payment ========== */

        conf.addTransformer("Payment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            if (val.paymentPreferenceId) {
                Map<String, Object> paymentIdByOppId = getMappingCache("PaymentIdByOppId")
                paymentIdByOppId.put((String) val.paymentPreferenceId, val.paymentId)
            }

            // load after FinAccountTrans, both depend on the other but Payment is mutable and FinancialAccountTrans in Mantle is not (create only)
            et.addEntry(new SimpleEntry("mantle.account.payment.Payment", [paymentId:val.paymentId,
                    paymentTypeEnumId:map('paymentTypeId', (String) val.paymentTypeId), fromPartyId:val.partyIdFrom, toPartyId:val.partyIdTo,
                    paymentInstrumentEnumId:map('paymentInstrumentEnumId', (String) val.paymentMethodTypeId),
                    paymentMethodId:val.paymentMethodId, statusId:map('paymentStatusId', (String) val.statusId),
                    effectiveDate:val.effectiveDate, amount:val.amount, amountUomId:val.currencyUomId,
                    paymentRefNum:(val.checkNum ?: val.paymentRefNum), comments:(val.checkNum && val.paymentRefNum ? 'Ref: ' + val.paymentRefNum + (val.comments ? '; ' + val.comments : '') : val.comments),
                    finAccountTransId:val.finAccountTransId, overrideGlAccountId:map('glAccountId', (String) val.overrideGlAccountId),
                    originalCurrencyAmount:val.actualCurrencyAmount, originalCurrencyUomId:val.actualCurrencyUomId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: doing nothing with paymentGatewayResponseId (only ref from PaymentGatewayResponse to Payment in Mantle), paymentPreferenceId
        }})
        conf.addTransformer("PaymentApplication", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            EntityValue payment = Moqui.executionContext.entity.find("mantle.account.payment.Payment").condition("paymentId", val.paymentId).one()
            et.addEntry(new SimpleEntry("mantle.account.payment.PaymentApplication", [paymentApplicationId:val.paymentApplicationId,
                    paymentId:val.paymentId, invoiceId:val.invoiceId, invoiceItemSeqId:val.invoiceItemSeqId,
                    billingAccountId:val.billingAccountId, toPaymentId:val.toPaymentId, taxAuthGeoId:val.taxAuthGeoId,
                    overrideGlAccountId:map('glAccountId', (String) val.overrideGlAccountId),
                    amountApplied:val.amountApplied, appliedDate:(payment?.effectiveDate ?: ((String) val.lastUpdatedTxStamp).take(23)),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("PaymentGatewayResponse", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // after Payment and OrderPaymentPreference
            // must be after Payment so PaymentIdByOppId cache is populated
            Map<String, Object> paymentIdByOppId = getMappingCache("PaymentIdByOppId")
            String paymentId = paymentIdByOppId.get((String) val.orderPaymentPreferenceId)
            if (!paymentId) paymentId = 'OPP' + ((String) val.orderPaymentPreferenceId)
            EntityValue payment = Moqui.executionContext.entity.find("mantle.account.payment.Payment").condition("paymentId", paymentId).one()
            if (payment == null) {
                logger.warn("Skipping GatewayResponse ${val.paymentGatewayResponseId} OPP ${val.orderPaymentPreferenceId} type ${val.paymentServiceTypeEnumId} msg: ${val.gatewayMessage}")
                et.loadCurrent(false)
                return
            }

            et.addEntry(new SimpleEntry("mantle.account.method.PaymentGatewayResponse", [
                    paymentGatewayResponseId:val.paymentGatewayResponseId,
                    paymentOperationEnumId:map('paymentServiceTypeEnumId', (String) val.paymentServiceTypeEnumId),
                    paymentId:paymentId, paymentMethodId:val.paymentMethodId,
                    amount:val.amount, amountUomId:val.currencyUomId, referenceNum:val.referenceNum, altReference:val.altReference,
                    subReference:val.subReference, responseCode:val.gatewayCode, reasonCode:val.gatewayFlag,
                    avsResult:val.gatewayAvsResult, cvResult:val.gatewayCvResult, scoreResult:val.gatewayScoreResult,
                    reasonMessage:val.gatewayMessage, transactionDate:val.transactionDate, resultDeclined:val.resultDeclined,
                    resultNsf:val.resultNsf, resultBadExpire:val.resultBadExpire, resultBadCardNumber:val.resultBadCardNumber,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        // NOTE: PaymentCreditType and PaymentCredit are custom entities for adjusting Payment
        conf.addTransformer("PaymentCreditType", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.loadCurrent(false)
            Map<String, Object> paymentCreditTypeCache = getMappingCache("PaymentCreditType")
            paymentCreditTypeCache.put((String) val.creditTypeId, val)
        }})
        conf.addTransformer("PaymentCredit", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // run after PaymentApplication
            et.loadCurrent(false)
            Map<String, Object> paymentCreditTypeCache = getMappingCache("PaymentCreditType")
            Map<String, Object> paymentCreditType = (Map<String, Object>) paymentCreditTypeCache.get((String) val.creditTypeId, val)
            String overrideGlAccountId = map('glAccountId', (String) val.overrideGlAccountId ?: (String) paymentCreditType.defaultGlAccountId)
            String paymentId = val.paymentId
            String description = paymentCreditType.description
            BigDecimal amount = new BigDecimal((String) val.amount)

            EntityValue payment = Moqui.executionContext.entity.find("mantle.account.payment.Payment").condition("paymentId", paymentId).one()
            if (((String) payment.statusId) in ["PmntCancelled", "PmntVoid", "PmntDeclined", "PmntRefunded"]) return
            EntityList paymentAppls = Moqui.executionContext.entity.find("mantle.account.payment.PaymentApplication")
                    .condition("paymentId", paymentId).condition("invoiceId", "!=", null).orderBy("-amountApplied").list()
            if (paymentAppls.size() == 0) {
                logger.warn("No PaymentApplication found for Payment ${paymentId} in PaymentCredit ${val.creditId}, creating PaymentApplication with overrideGlAccountId")
                // NOTE: using negative amount here as records like this are used to zero out the payment, post directly to gl account
                et.addEntry(new SimpleEntry("mantle.account.payment.PaymentApplication", [paymentApplicationId:('PCDT' + val.creditId),
                        paymentId:paymentId, amountApplied:-amount, overrideGlAccountId:overrideGlAccountId,
                        appliedDate:((String) val.lastUpdatedTxStamp).take(23),
                        lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
                return
            }
            EntityValue paymentAppl = paymentAppls[0]
            String invoiceId = paymentAppl.invoiceId
            if (paymentAppls.size() > 1) logger.warn("Multiple (${paymentAppls.size()}) PaymentApplication records found for Payment ${paymentId} in PaymentCredit ${val.creditId}, adjusting first Invoice ${invoiceId}")

            paymentAppl.amountOriginallyApplied = paymentAppl.amountApplied
            BigDecimal applAmountApplied = (BigDecimal) paymentAppl.amountApplied
            if (amount > applAmountApplied) {
                BigDecimal diff = amount - applAmountApplied
                payment.amount = ((BigDecimal) payment.amount) + diff
                payment.update()
                amount = applAmountApplied
            }
            paymentAppl.amountApplied = applAmountApplied - amount
            paymentAppl.update()
            // if (applAmountApplied < 0.0) logger.error("Adjusted PaymentApplication ${paymentAppl.paymentApplicationId} for PaymentCredit ${val.creditId} Payment ${paymentId} to less than zero ${paymentAppl.amountApplied}")

            et.addEntry(new SimpleEntry("mantle.account.invoice.InvoiceItem", [invoiceId:invoiceId,
                    invoiceItemSeqId:null, itemTypeEnumId:'ItemInvAdjust', quantity:'1', amount:-amount, description:description,
                    overrideGlAccountId:overrideGlAccountId, itemDate:((String) val.lastUpdatedTxStamp).take(23),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== PaymentMethod ========== */

        conf.addTransformer("PaymentMethod", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String paymentMethodTypeEnumId = map('paymentMethodTypeEnumId', (String) val.paymentMethodTypeId)
            // if (!(paymentMethodTypeEnumId in ['PmtCreditCard', 'PmtBankAccount'])) { et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.account.method.PaymentMethod", [paymentMethodId:val.paymentMethodId,
                    paymentMethodTypeEnumId:paymentMethodTypeEnumId, ownerPartyId:val.partyId, description:val.description,
                    fromDate:val.fromDate, thruDate:val.thruDate, overrideGlAccountId:map('glAccountId', (String) val.glAccountId),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: skipping finAccountId for now, needs FinAccount transformer
            if (paymentMethodTypeEnumId == 'PmtBankAccount') {
                // NOTE: this could come from EftAccount records, but also mapping to this type for OFBiz types other than EftAccount
                et.addEntry(new SimpleEntry("mantle.account.method.BankAccount", [paymentMethodId:val.paymentMethodId,
                        typeEnumId:'BatChecking', lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            }
        }})
        conf.addTransformer("CreditCard", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.account.method.PaymentMethod", [paymentMethodId:val.paymentMethodId,
                    companyNameOnAccount:val.companyNameOnCard, titleOnAccount:val.titleOnCard,
                    firstNameOnAccount:val.firstNameOnCard, middleNameOnAccount:val.middleNameOnCard,
                    lastNameOnAccount:val.lastNameOnCard, suffixOnAccount:val.suffixOnCard, postalContactMechId:val.contactMechId]))
            et.addEntry(new SimpleEntry("mantle.account.method.CreditCard", [paymentMethodId:val.paymentMethodId,
                    creditCardTypeEnumId:map('cardType', ((String) val.cardType)), cardNumber:val.cardNumber,
                    validFromDate:val.validFromDate, expireDate:val.expireDate, issueNumber:val.issueNumber,
                    consecutiveFailedAuths:val.consecutiveFailedAuths, lastFailedAuthDate:val.lastFailedAuthDate,
                    consecutiveFailedNsf:val.consecutiveFailedNsf, lastFailedNsfDate:val.lastFailedNsfDate,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== Product ========== */

        conf.addTransformer("Product", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String productTypeEnumId = OFBizFieldMap.get('productTypeEnumId', (String) val.productTypeId)
            if (!productTypeEnumId) { logger.info("Skipping Product ${val.productId} of type ${val.productTypeId}"); et.loadCurrent(false); return }
            // NOTE: this is a very simple transform, not yet near complete for Product
            EntityValue product = Moqui.executionContext.entity.find("mantle.product.Product").condition("productId", val.productId).one()
            if (product != null) { logger.info("Skipping Product ${val.productId}, already exists"); et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.product.Product", [productId:val.productId, productTypeEnumId:productTypeEnumId,
                    assetTypeEnumId:OFBizFieldMap.get('assetTypeEnumId', (String) val.productTypeId),
                    assetClassEnumId:OFBizFieldMap.get('assetClassEnumId', (String) val.productTypeId),
                    productName:(val.productName ?: val.internalName), description:val.description, comments:val.comments,
                    salesIntroductionDate:val.introductionDate, salesDiscontinuationDate:val.salesDiscontinuationDate,
                    supportDiscontinuationDate:val.supportDiscontinuationDate, salesDiscWhenNotAvail:val.salesDiscWhenNotAvail,
                    requireInventory:val.requireInventory, amountFixed:val.quantityIncluded, amountRequire:val.requireAmount,
                    amountUomId:(val.quantityUomId == 'OTH_pk' ? 'OTH_ct' : val.quantityUomId),
                    taxable:val.taxable, chargeShipping:val.chargeShipping, inShippingBox:val.inShippingBox,
                    returnable:val.returnable, originGeoId:val.originGeoId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            if (val.weight) et.addEntry(new SimpleEntry("mantle.product.ProductDimension", [productId:val.productId,
                    dimensionTypeId:'Weight', value:val.weight, valueUomId:val.weightUomId]))
            if (val.shippingWeight) et.addEntry(new SimpleEntry("mantle.product.ProductDimension", [productId:val.productId,
                    dimensionTypeId:'ShippingWeight', value:val.shippingWeight, valueUomId:val.weightUomId]))
            if (val.quantityIncluded) et.addEntry(new SimpleEntry("mantle.product.ProductDimension", [productId:val.productId,
                    dimensionTypeId:'QuantityIncluded', value:val.quantityIncluded, valueUomId:(val.quantityUomId == 'OTH_pk' ? 'OTH_ct' : val.quantityUomId)]))
            if (val.piecesIncluded) et.addEntry(new SimpleEntry("mantle.product.ProductDimension", [productId:val.productId,
                    dimensionTypeId:'PiecesIncluded', value:val.piecesIncluded, valueUomId:'OTH_ea']))
        }})
        conf.addTransformer("ProductPrice", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String productPriceId = ((String) val.productId) + '_' + ((String) val.productPriceTypeId).substring(0,1) + '_' + ((String) val.currencyUomId) + '_' + ((String) val.productStoreGroupId) + '_' + ((String) val.fromDate).substring(20)
            if (val.productPricePurposeId != 'PURCHASE') { logger.info("Skipping ProductPrice ${productPriceId} of purpose ${val.productPricePurposeId}"); et.loadCurrent(false); return }
            String priceTypeEnumId = OFBizFieldMap.get('productPriceTypeId', (String) val.productPriceTypeId)
            if (!priceTypeEnumId) { logger.info("Skipping ProductPrice ${productPriceId} of type ${val.productPriceTypeId}"); et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.product.ProductPrice", [productPriceId:productPriceId, productId:val.productId,
                    priceTypeEnumId:priceTypeEnumId, pricePurposeEnumId:'PppPurchase', fromDate:val.fromDate, thruDate:val.thruDate,
                    price:val.price, priceUomId:val.currencyUomId, minQuantity:1.0, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== Shipment ========== */

        conf.addTransformer("Shipment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.shipment.Shipment", [shipmentId:val.shipmentId,
                    shipmentTypeEnumId:map('shipmentTypeId', (String) val.shipmentTypeId),
                    statusId:map('shipmentStatusId', (String) val.statusId), fromPartyId:val.partyIdFrom, toPartyId:val.partyIdTo,
                    binLocationNumber:((String) val.picklistBinId)?.isLong() ? val.picklistBinId : null,
                    entryDate:((String) val.createdTxStamp).take(23), estimatedReadyDate:val.estimatedReadyDate,
                    estimatedShipDate:val.estimatedShipDate, estimatedArrivalDate:val.estimatedArrivalDate,
                    latestCancelDate:val.latestCancelDate, estimatedShipCost:val.estimatedShipCost, costUomId:val.currencyUomId,
                    handlingInstructions:val.handlingInstructions, addtlShippingCharge:val.additionalShippingCharge,
                    addtlShippingChargeDesc:val.addtlShippingChargeDesc, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // WorkEffort not yet transformed: shipWorkEffortId:val.estimatedShipWorkEffId, receiveWorkEffortId:val.estimatedArrivalWorkEffId,
            // NOTE: these redundant fields do not exist in mantle: primaryOrderId, primaryReturnId, primaryShipGroupSeqId, originFacilityId, destinationFacilityId, originContactMechId, originTelecomNumberId, destinationContactMechId, destinationTelecomNumberId
        }})
        conf.addTransformer("ShipmentBoxType", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentBoxType", val + ([lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)] as Map<String, Object>)))
        }})
        conf.addTransformer("ShipmentItem", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            if (!val.productId) { logger.warn("Skipping shipment ${val.shipmentId} item ${val.shipmentItemSeqId} with no productId"); et.loadCurrent(false); return }
            Map<String, Object> siMappingCache = getMappingCache("ShipmentItemProduct")
            siMappingCache.put(((String) val.shipmentId) + ':' + ((String) val.shipmentItemSeqId), val.productId)
            EntityValue existingItem = Moqui.executionContext.entity.find("mantle.shipment.ShipmentItem")
                    .condition("shipmentId", val.shipmentId).condition("productId", val.productId).one()
            if (existingItem != null) {
                // this is a bit of a hack, won't produce correct output if loading into a file instead of to the DB
                existingItem.quantity = ((BigDecimal) existingItem.quantity) + (val.quantity as BigDecimal)
                existingItem.update()
                logger.warn("Adding quantity from shipment ${val.shipmentId} item ${val.shipmentItemSeqId} with productId ${val.productId} that was in another item")
            } else {
                et.addEntry(new SimpleEntry("mantle.shipment.ShipmentItem", [shipmentId:val.shipmentId, productId:val.productId,
                        quantity:val.quantity, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            }
        }})
        conf.addTransformer("ShipmentPackage", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentPackage", [shipmentId:val.shipmentId,
                    shipmentPackageSeqId:val.shipmentPackageSeqId, shipmentBoxTypeId:val.shipmentBoxTypeId,
                    weight:val.weight, weightUomId:val.weightUomId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("ShipmentRouteSegment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentRouteSegment", [shipmentId:val.shipmentId,
                    shipmentRouteSegmentSeqId:val.shipmentRouteSegmentId, deliveryId:val.deliveryId,
                    originFacilityId:val.originFacilityId, originPostalContactMechId:val.originContactMechId,
                    originTelecomContactMechId:val.originTelecomNumberId, destinationFacilityId:val.destFacilityId,
                    destPostalContactMechId:val.destContactMechId, destTelecomContactMechId:val.destTelecomNumberId,
                    shipmentMethodEnumId:map('shipmentMethodTypeId', (String) val.shipmentMethodTypeId),
                    carrierPartyId:val.carrierPartyId, statusId:map('carrierServiceStatusId', (String) val.carrierServiceStatusId),
                    carrierDeliveryZone:val.carrierDeliveryZone, carrierRestrictionCodes:val.carrierRestrictionCodes,
                    carrierRestrictionDesc:val.carrierRestrictionDesc, billingWeight:val.billingWeight,
                    billingWeightUomId:val.billingWeightUomId, actualTransportCost:val.actualTransportCost,
                    actualServiceCost:val.actualServiceCost, actualOtherCost:val.actualOtherCost, actualCost:val.actualCost,
                    costUomId:val.currencyUomId, actualStartDate:val.actualStartDate, actualArrivalDate:val.actualArrivalDate,
                    estimatedStartDate:val.estimatedStartDate, estimatedArrivalDate:val.estimatedArrivalDate,
                    trackingIdNumber:val.trackingIdNumber, trackingDigest:val.trackingDigest, homeDeliveryType:val.homeDeliveryType,
                    homeDeliveryDate:val.homeDeliveryDate, thirdPartyAccountNumber:val.thirdPartyAccountNumber,
                    thirdPartyPostalCode:val.thirdPartyPostalCode, thirdPartyCountryGeoCode:val.thirdPartyCountryGeoCode,
                    highValueReport:val.upsHighValueReport, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("ShipmentPackageContent", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            Map<String, Object> siMappingCache = getMappingCache("ShipmentItemProduct")
            String productId = siMappingCache.get(((String) val.shipmentId) + ':' + ((String) val.shipmentItemSeqId))
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentPackageContent", [shipmentId:val.shipmentId,
                    shipmentPackageSeqId:val.shipmentPackageSeqId, productId:productId,
                    quantity:val.quantity, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("ShipmentPackageRouteSeg", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentPackageRouteSeg", [shipmentId:val.shipmentId,
                    shipmentPackageSeqId:val.shipmentPackageSeqId, shipmentRouteSegmentSeqId:val.shipmentRouteSegmentId,
                    trackingCode:val.trackingCode, boxNumber:val.boxNumber, labelImage:val.labelImage,
                    labelIntlSignImage:val.labelIntlSignImage, labelHtml:val.labelHtml, labelPrinted:val.labelPrinted,
                    internationalInvoice:val.internationalInvoice,
                    packageTransportAmount:val.packageTransportCost, packageServiceAmount:val.packageServiceCost,
                    packageOtherAmount:val.packageOtherCost, codAmount:val.codAmount, insuredAmount:val.insuredAmount,
                    amountUomId:val.currencyUomId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        conf.addTransformer("OrderShipment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String orderPartSeqId = "00001" // optimized for only single part orders
            Map<String, Object> siMappingCache = getMappingCache("ShipmentItemProduct")
            String productId = siMappingCache.get(((String) val.shipmentId) + ':' + ((String) val.shipmentItemSeqId))
            et.addEntry(new SimpleEntry("mantle.shipment.ShipmentItemSource", [shipmentItemSourceId:UUID.randomUUID().toString(),
                    shipmentId:val.shipmentId, productId:productId, quantity:val.quantity, orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    orderItemSeqId:val.orderItemSeqId, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})
        // not doing ShipmentItemBilling (shipmentId, shipmentItemSeqId, invoiceId, invoiceItemSeqId), would need to be merged with ShipmentItemSource records from OrderShipment

        /* =========== Custom Transformer Examples =========== */

        conf.addTransformer("PartyClassification", new Transformer() { void transform(EntryTransform et) {
            Map<String, Object> val = et.entry.etlValues
            String partyClassificationGroupId = val.partyClassificationGroupId
            if (customerClasses.containsKey(partyClassificationGroupId)) {
                String partyClassificationId = customerClasses.get(partyClassificationGroupId)
                long applCount = Moqui.executionContext.entity.find("mantle.party.PartyClassificationAppl")
                        .condition("partyId", val.partyId).condition("partyClassificationId", partyClassificationId).count()
                if (applCount > 0) {
                    et.loadCurrent(false)
                } else {
                    et.addEntry(new SimpleEntry("mantle.party.PartyClassificationAppl", [partyId:val.partyId,
                            partyClassificationId:partyClassificationId, fromDate:val.fromDate, thruDate:val.thruDate,
                            lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
                }
            } else if (settlementTermByClass.containsKey(partyClassificationGroupId)) {
                String agreementId = (String) val.partyId + '_' + partyClassificationGroupId
                et.addEntry(new SimpleEntry("mantle.party.agreement.Agreement", [agreementId:agreementId, agreementTypeEnumId:'AgrSales',
                        organizationPartyId:'Company', otherPartyId:val.partyId, otherRoleTypeId:'Customer', description:'Customer Terms',
                        agreementDate:val.fromDate, fromDate:val.fromDate, thruDate:val.thruDate, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
                et.addEntry(new SimpleEntry("mantle.party.agreement.AgreementTerm", [agreementTermId:agreementId+'_CT', agreementId:agreementId,
                        settlementTermId:settlementTermByClass.get(partyClassificationGroupId), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)] as Map<String, Object>))
            } else {
                et.loadCurrent(false)
            }
        }})
    }

    // =========== Custom Mappings ===========
    static Map<String, String> customerClasses = ['10000':'CustWholesale', '10001':'CustDistributor', '10002':'CustEnterprise']
    static Map<String, String> settlementTermByClass = ['10010':'Net45Disc2Pct15', 'PCG_CASH':'Immediate', 'PCG_NET30':'Net30']
}
