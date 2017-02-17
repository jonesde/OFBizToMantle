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
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.etl.SimpleEtl
import org.moqui.etl.SimpleEtl.EntryTransform
import org.moqui.etl.SimpleEtl.SimpleEntry
import org.moqui.etl.SimpleEtl.Transformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static mantle.ofbiz.OFBizFieldMap.map

@CompileStatic
class OFBizTransform {
    static Logger logger = LoggerFactory.getLogger(OFBizTransform.class)

    static List<List<String>> loadOrderParallel = [
            ['Party', 'ContactMech', 'Product', 'Lot', 'OrderHeader'],
            ['Person', 'PartyGroup', 'PartyRole', 'UserLogin',
                    'PartyContactMechPurpose', 'PostalAddress', 'TelecomNumber', 'PaymentMethod',
                    'ProductPrice', 'InventoryItem', 'PhysicalInventory',
                    'OrderItemShipGroup'],
            ['PartyClassification', 'PartyRelationship', 'CreditCard',
                    'OrderRole', 'OrderContactMech', 'OrderItem'],
            ['OrderAdjustment', 'OrderPaymentPreference', 'OrderItemShipGrpInvRes']
            /* 'ItemIssuance', 'ShipmentReceipt' */
            /* 'InventoryItemDetail' */
    ]

    static SimpleEtl.TransformConfiguration conf = new SimpleEtl.TransformConfiguration()
    static {
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
            }
            et.addEntry(new SimpleEntry("mantle.party.contact.PostalAddress", entryMap + ([stateProvinceGeoId:stateProvinceGeoId,
                    unitNumber:((String) val.houseNumber + (val.houseNumberExt ? '-' + (String) val.houseNumberExt : '')),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)] as Map<String, Object>)))
        }})
        conf.addTransformer("TelecomNumber", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String cNum = val.contactNumber
            if (!cNum) { et.loadCurrent(false); return }
            cNum = cNum.trim()
            if (!cNum.contains("-") && cNum.length() == 7) cNum = cNum.substring(0,3) + '-' + cNum.substring(3,7)
            et.addEntry(new SimpleEntry("mantle.party.contact.TelecomNumber", [contactMechId:val.contactMechId,
                    countryCode:val.countryCode, areaCode:val.areaCode, contactNumber:cNum,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== Inventory ========== */

        conf.addTransformer("Lot", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.product.asset.Lot", [lotId:val.lotId, creationDate:val.creationDate,
                    manufacturedDate:val.creationDate, quantity:val.quantity, expirationDate:((String) val.expirationDate)?.take(10),
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp)?.take(23)]))
        }})
        conf.addTransformer("InventoryItem", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // some example key cleanup
            String locationSeqId = val.locationSeqId
            if (locationSeqId) { int spaceIdx = locationSeqId.indexOf(" "); if (spaceIdx > 0) locationSeqId = locationSeqId.substring(0, spaceIdx) }
            et.addEntry(new SimpleEntry("mantle.product.asset.Asset", [assetId:val.inventoryItemId, productId:val.productId,
                    hasQuantity:(val.inventoryItemTypeId == 'SERIALIZED_INV_ITEM' ? 'N' : 'Y'), ownerPartyId:val.ownerPartyId,
                    facilityId:val.facilityId, locationSeqId:locationSeqId, statusId:map('inventoryStatusId', (String) val.statusId),
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
            et.addEntry(new SimpleEntry("mantle.product.asset.AssetDetail", [
                    assetDetailId:((String) val.inventoryItemId) + ((String) val.inventoryItemDetailSeqId),
                    assetId:val.inventoryItemId, productId:productId, effectiveDate:val.effectiveDate, unitCost:val.unitCost,
                    quantityOnHandDiff:val.quantityOnHandDiff, availableToPromiseDiff:val.availableToPromiseDiff,
                    shipmentId:val.shipmentId, returnId:val.returnId, returnItemSeqId:val.returnItemSeqId,
                    assetIssuanceId:val.itemIssuanceId, assetReceiptId:val.receiptId, physicalInventoryId:val.physicalInventoryId,
                    varianceReasonEnumId:map('varianceReasonEnumId', (String) val.reasonEnumId), description:val.description,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // NOTE: workEffortId not yet handled, need to also transform WorkEffort for this
        }})

        conf.addTransformer("PhysicalInventory", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.product.asset.PhysicalInventory", [physicalInventoryId:val.physicalInventoryId,
                    physicalInventoryDate:val.physicalInventoryDate, partyId:val.partyId, comments:val.generalComments,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        conf.addTransformer("OrderItemShipGrpInvRes", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // needs: Asset, OrderItem
            String productId = Moqui.executionContext.entity.find("mantle.product.asset.Asset").condition("assetId", val.inventoryItemId).one()?.productId
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
            et.addEntry(new SimpleEntry("mantle.product.issuance.AssetReceipt", [assetReceiptId:val.receiptId,
                    assetId:val.inventoryItemId, productId:val.productId, orderId:val.orderId, orderItemSeqId:val.orderItemSeqId,
                    shipmentId:val.shipmentId, shipmentPackageSeqId:val.shipmentPackageSeqId, returnId:val.returnId,
                    returnItemSeqId:val.returnItemSeqId, rejectionReasonEnumId:map('rejectionId', (String) val.rejectionId),
                    receivedByUserId:val.receivedByUserLoginId, receivedDate:val.datetimeReceived, itemDescription:val.itemDescription,
                    quantityAccepted:val.quantityAccepted, quantityRejected:val.quantityRejected,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
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
            et.addEntry(new SimpleEntry("mantle.order.OrderPart", [orderId:val.orderId,
                    orderPartSeqId:((String) val.shipGroupSeqId).padLeft(5, '0'),
                    shipmentMethodEnumId:map('shipmentMethodTypeId', (String) val.shipmentMethodTypeId),
                    vendorPartyId:val.vendorPartyId/*usually null, expected from OrderRole*/, carrierPartyId:val.carrierPartyId,
                    facilityId:val.facilityId, postalContactMechId:val.contactMechId, telecomContactMechId:val.telecomContactMechId,
                    trackingNumber:val.trackingNumber, shippingInstructions:val.shippingInstructions, maySplit:val.maySplit,
                    giftMessage:val.giftMessage, isGift:val.isGift, shipAfterDate:val.shipAfterDate, shipBeforeDate:val.shipByDate,
                    estimatedShipDate:val.estimatedShipDate, estimatedDeliveryDate:val.estimatedDeliveryDate,
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
            et.addEntry(new SimpleEntry("mantle.order.OrderItem", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    orderItemSeqId:val.orderItemSeqId, productId:val.productId, otherPartyProductId:val.supplierProductId,
                    itemTypeEnumId:map('orderItemTypeId', (String) val.orderItemTypeId),
                    productFeatureId:val.productFeatureId, productCategoryId:val.productCategoryId, comments:val.comments,
                    itemDescription:val.itemDescription, quantity:val.quantity, quantityCancelled:val.cancelQuantity,
                    selectedAmount:val.selectedAmount, requiredByDate:val.shipBeforeDate,
                    unitAmount:val.unitPrice, unitListPrice:val.unitListPrice, isModifiedPrice:val.isModifiedPrice,
                    externalItemSeqId:val.externalId, fromAssetId:val.fromInventoryItemId, isPromo:val.isPromo,
                    subscriptionId:val.subscriptionId, salesOpportunityId:val.salesOpportunityId,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // TODO: overrideGlAccountId when glAccount mapping done
        }})
        // NOTE: nothing for OrderItemShipGroupAssoc, doing single part orders only
        conf.addTransformer("OrderAdjustment", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup
            String orderPartSeqId = "00001" // optimized for only single part orders
            et.addEntry(new SimpleEntry("mantle.order.OrderItem", [orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    orderItemSeqId:val.orderAdjustmentId, itemTypeEnumId:map('orderAdjustmentTypeId', (String) val.orderAdjustmentTypeId),
                    parentItemSeqId:(val.orderItemSeqId ?: val.originalAdjustmentId), comments:val.comments, itemDescription:val.description,
                    unitAmount:val.amount, amountAlreadyIncluded:val.amountAlreadyIncluded, productFeatureId:val.productFeatureId,
                    sourceReferenceId:val.sourceReferenceId, sourcePercentage:val.sourcePercentage,
                    customerReferenceId:val.customerReferenceId, exemptAmount:val.exemptAmount,
                    isPromo:(val.orderAdjustmentTypeId == 'PROMOTION_ADJUSTMENT' ? 'Y' : 'N'), isModifiedPrice:val.isManual,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            // TODO: overrideGlAccountId when glAccount mapping done
            // NOTE: doesn't map to TaxAuthority.taxAuthorityId, could look up by taxAuthGeoId, taxAuthPartyId
        }})
        conf.addTransformer("OrderPaymentPreference", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            // must be after OrderHeader and OrderItemShipGroup, and for from/to parties after OrderRole
            String orderPartSeqId = "00001" // optimized for only single part orders
            EntityValue orderPart = Moqui.executionContext.entity.find("mantle.order.OrderPart").condition("orderId", val.orderId)
                    .condition("orderPartSeqId", orderPartSeqId).one()
            et.addEntry(new SimpleEntry("mantle.account.payment.Payment", [paymentId:'OPP' + ((String) val.orderPaymentPreferenceId),
                    paymentTypeEnumId:'PtInvoicePayment', orderId:val.orderId, orderPartSeqId:orderPartSeqId,
                    fromPartyId:orderPart?.customerPartyId, toPartyId:orderPart?.vendorPartyId,
                    paymentInstrumentEnumId:map('paymentInstrumentEnumId', (String) val.paymentMethodTypeId),
                    paymentMethodId:val.paymentMethodId, finAccountId:val.finAccountId, presentFlag:val.presentFlag,
                    swipedFlag:val.swipedFlag, amount:val.maxAmount, processAttempt:val.processAttempt, needsNsfRetry:val.needsNsfRetry,
                    paymentAuthCode:val.manualAuthCode, paymentRefNum:val.manualRefNum,
                    statusId:map('paymentStatusId', (String) val.statusId), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

/*

 */

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
        // TODO: how to handle PartyNote combined with NoteData? split easy, combine another question
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
            if ("_NA_".equals(roleTypeId)) { et.loadCurrent(false); return }
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
            if (currentPassword != null) {
                int hashEnd = -1
                if (currentPassword.startsWith('$')) hashEnd = currentPassword.indexOf('$', 1)
                else if (currentPassword.startsWith('{')) hashEnd = currentPassword.indexOf('}', 1)
                if (hashEnd > 0) {
                    passwordHashType = currentPassword.substring(1, hashEnd)
                    currentPassword = currentPassword.substring(hashEnd + 1)
                }
            }
            et.addEntry(new SimpleEntry("moqui.security.UserAccount", [userId:val.userLoginId, username:val.userLoginId,
                partyId:val.partyId, passwordHint:val.passwordHint, requirePasswordChange:val.requirePasswordChange,
                disabled:(val.enabled == 'N' ? 'Y' : 'N'), disabledDateTime:val.disabledDateTime, successiveFailedLogins:val.successiveFailedLogins,
                currencyUomId:val.lastCurrencyUom, locale:val.lastLocale, timeZone:val.lastTimeZone,
                currentPassword:currentPassword, passwordHashType:passwordHashType, passwordSetDate:((String) val.lastUpdatedTxStamp).take(23),
                lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
        }})

        /* ========== PaymentMethod ========== */

        conf.addTransformer("PaymentMethod", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            String paymentMethodTypeEnumId = OFBizFieldMap.get('paymentMethodTypeEnumId', (String) val.paymentMethodTypeId)
            if (!paymentMethodTypeEnumId) { logger.info("Skipping PaymentMethod ${val.paymentMethodId} of type ${val.paymentMethodTypeId}"); et.loadCurrent(false); return }
            et.addEntry(new SimpleEntry("mantle.account.method.PaymentMethod", [paymentMethodId:val.paymentMethodId,
                    paymentMethodTypeEnumId:paymentMethodTypeEnumId,
                    ownerPartyId:val.partyId, description:val.description, fromDate:val.fromDate, thruDate:val.thruDate,
                    lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
            if (val.glAccountId) logger.warn("Found glAccountId ${val.glAccountId} in PaymentMethod ${val.paymentMethodId}")
            // TODO: skipping glAccountId for now, needs GlAccount mapping
            // NOTE: skipping finAccountId for now, needs FinAccount transformer
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
            et.addEntry(new SimpleEntry("mantle.product.Product", [productId:val.productId, productTypeEnumId:productTypeEnumId,
                    assetTypeEnumId:OFBizFieldMap.get('productTypeEnumId', (String) val.productTypeId),
                    assetClassEnumId:OFBizFieldMap.get('productTypeEnumId', (String) val.productTypeId),
                    productName:val.productName, description:val.description, comments:val.comments,
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

        /* =========== Custom Transformer Examples =========== */

        conf.addTransformer("PartyClassification", new Transformer() { void transform(EntryTransform et) {
            Map<String, Object> val = et.entry.etlValues
            String partyClassificationGroupId = val.partyClassificationGroupId
            if (customerClasses.containsKey(partyClassificationGroupId)) {
                et.addEntry(new SimpleEntry("mantle.party.PartyClassificationAppl", [partyId:val.partyId,
                        partyClassificationId:customerClasses.get(partyClassificationGroupId),
                        fromDate:val.fromDate, thruDate:val.thruDate, lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
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
