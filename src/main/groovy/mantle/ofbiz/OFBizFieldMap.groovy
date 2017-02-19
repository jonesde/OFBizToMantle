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

@CompileStatic
class OFBizFieldMap {
    static String map(String field, String value) {
        LinkedHashMap<String, String> valueMap = fieldValueMaps.get(field)
        if (valueMap == null) return value
        return valueMap.containsKey(value) ? valueMap.get(value) : value
    }
    static String get(String field, String value) {
        LinkedHashMap<String, String> valueMap = fieldValueMaps.get(field)
        if (valueMap == null) return null
        return valueMap.get(value)
    }
    static LinkedHashMap<String, LinkedHashMap<String, String>> fieldValueMaps = [
        // ContactMech
        contactMechPurposeTypeId:[BILLING_EMAIL:'EmailBilling', BILLING_LOCATION:'PostalBilling', FACEBOOK_URL:'WebUrlFacebook',
                FAX_BILLING:'PhoneBillingFax', FAX_NUMBER:'PhoneFax', FAX_NUMBER_SEC:'PhoneFax', FAX_SHIPPING:'PhoneShippingFax',
                GENERAL_LOCATION:'PostalPrimary', ICAL_URL:'WebUrlICal', LINKEDIN_URL:'WebUrlLinkedIn',
                MARKETING_EMAIL:'EmailMarketing', ORDER_EMAIL:'EmailOrder', OTHER_EMAIL:'EmailOther', PAYMENT_EMAIL:'EmailPayment',
                PAYMENT_LOCATION:'PostalPayment', PHONE_ASSISTANT:'PhoneAssistant', PHONE_BILLING:'PhoneBilling',
                PHONE_DID:'PhoneDid', PHONE_HOME:'PhoneHome', PHONE_MOBILE:'PhoneMobile', PHONE_PAYMENT:'PhonePayment',
                PHONE_QUICK:'PhoneQuick', PHONE_SHIP_ORIG:'PhoneShippingOrigin', PHONE_SHIPPING:'PhoneShippingDest',
                PHONE_WORK:'PhoneWork', PHONE_WORK_SEC:'PhoneWork', PREVIOUS_LOCATION:'PostalPrevious', PRIMARY_EMAIL:'EmailPrimary',
                PRIMARY_LOCATION:'PostalPrimary', PRIMARY_PHONE:'PhonePrimary', PRIMARY_WEB_URL:'WebUrlPrimary',
                PUR_RET_LOCATION:'PostalPurchReturn', SHIP_ORIG_LOCATION:'PostalShippingOrigin',
                SHIPPING_LOCATION:'PostalShippingDest', SUPPORT_EMAIL:'EmailSupport', TWITTER_URL:'WebUrlTwitter'],
        contactMechTypeId:[POSTAL_ADDRESS:'CmtPostalAddress', TELECOM_NUMBER:'CmtTelecomNumber',
                ELECTRONIC_ADDRESS:'CmtElectronicAddress', EMAIL_ADDRESS:'CmtEmailAddress', IP_ADDRESS:'CmtIpAddress',
                DOMAIN_NAME:'CmtDomainName', WEB_ADDRESS:'CmtWebAddress'],

        // Invoice
        invoiceTypeId:[COMMISSION_INVOICE:'InvoiceCommission', CUST_RTN_INVOICE:'InvoiceReturn', PAYROL_INVOICE:'InvoicePayroll',
                PURCHASE_INVOICE:'InvoiceSales', PURC_RTN_INVOICE:'InvoiceReturn', PUR_INV_TEMPLATE:'InvoiceTemplate',
                SALES_INVOICE:'InvoiceSales', SALES_INV_TEMPLATE:'InvoiceTemplate', TEMPLATE:'InvoiceTemplate'],
                // INTEREST_INVOICE, VENDOR_CREDIT_MEMO
        invoiceIncomingStatusId:[INVOICE_IN_PROCESS:'InvoiceIncoming', INVOICE_APPROVED:'InvoiceApproved', INVOICE_SENT:'InvoiceIncoming',
                INVOICE_RECEIVED:'InvoiceReceived', INVOICE_READY:'InvoiceReceived', INVOICE_PAID:'InvoicePmtSent',
                INVOICE_WRITEOFF:'InvoiceWriteOff', INVOICE_CANCELLED:'InvoiceCancelled'],
        invoiceOutgoingStatusId:[INVOICE_IN_PROCESS:'InvoiceInProcess', INVOICE_APPROVED:'InvoiceFinalized', INVOICE_SENT:'InvoiceSent',
                INVOICE_RECEIVED:'InvoiceSent', INVOICE_READY:'InvoiceFinalized', INVOICE_PAID:'InvoicePmtRecvd',
                INVOICE_WRITEOFF:'InvoiceWriteOff', INVOICE_CANCELLED:'InvoiceCancelled'],
        invoiceItemTypeId:[CRT_FPROD_ITEM:'ItemProduct', CRT_SHIPPING_ADJ:'ItemShipping', INV_FPROD_ITEM:'ItemProduct',
                INVOICE_ITM_ADJ:'ItemInvAdjust', INVOICE_SM_DISCR:'ItemInvAdjust', INV_PROD_ITEM:'ItemProduct',
                ITM_ADD_FEATURE:'ItemAddtlFeature', ITM_CHARGEBACK_ADJ:'ItemChargebackAdjust', ITM_COMPLIANCE_ADJ:'ItemComplianceFee',
                ITM_DISCOUNT_ADJ:'ItemDiscount', ITM_FEE:'ItemFee', ITM_MISC_CHARGE:'ItemMiscCharge', ITM_PROMOTION_ADJ:'ItemDiscount',
                ITM_REPLACE_ADJ:'ItemReplacement', ITM_SALES_TAX:'ItemSalesTax', ITM_SHIPPING_CHARGES:'ItemShipping',
                ITM_SHRINKAGE_ADJ:'ItemShrinkage', P_FEE:'ItemFee', PINV_DISCOUNT_ADJ:'ItemDiscount',
                PINV_FPROD_ITEM:'ItemInventory', PINV_MISC_CHARGE:'ItemFee', PINVOICE_ADJ:'ItemInvAdjust',
                PINVOICE_CREDIT_MEMO:'ItemInvAdjust', PINV_PROD_ITEM:'ItemInventory', PINV_PROMOTION_ADJ:'ItemDiscount',
                PITM_MISC_CHARGE:'ItemFee', SINVOICE_ITM_ADJ:'ItemInvAdjust'],
                // NOTE this is not nearly of all the roughly 150 invoice item types

        // Order
        salesChannelEnumId:[EMAIL_SALES_CHANNEL:'ScEmail', PHONE_SALES_CHANNEL:'ScPhone', UNKNWN_SALES_CHANNEL:'ScUnknown', WEB_SALES_CHANNEL:'ScWeb'],
        orderStatusId:[ORDER_APPROVED:'OrderApproved', ORDER_CANCELLED:'OrderCancelled', ORDER_COMPLETED:'OrderCompleted',
                ORDER_CREATED:'OrderOpen', ORDER_HOLD:'OrderHold', ORDER_PROCESSING:'OrderProcessing',
                ORDER_REJECTED:'OrderRejected', ORDER_SENT:'OrderSent'],
        shipmentMethodTypeId:[AIR:'ShMthNextDay', GROUND:'ShMthGround', NO_SHIPPING:null, STANDARD:'ShMthGround', 'USPS-CAN':'ShMthGround', 'USPS Free':'ShMthGround'],
        orderItemTypeId:[ASSET_ORDER_ITEM:'ItemAsset', BULK_ORDER_ITEM:'ItemInventory', INVENTORY_ORDER_ITEM:'ItemInventory',
                PRODUCT_ORDER_ITEM:'ItemProduct', PURCHASE_SPECIFIC:'ItemInventory', RENTAL_ORDER_ITEM:'ItemRental',
                SUPPLIES_ORDER_ITEM:'ItemExpOfficeSup', WORK_ORDER_ITEM:'ItemWorkEffort'],
        orderAdjustmentTypeId:[ADDITIONAL_FEATURE:'ItemAddtlFeature', DISCOUNT_ADJUSTMENT:'ItemDiscount', FEE:'ItemFee',
                MISCELLANEOUS_CHARGE:'ItemMiscCharge', PROMOTION_ADJUSTMENT:'ItemDiscount', REPLACE_ADJUSTMENT:'ItemReplacement',
                SALES_TAX:'ItemSalesTax', SHIPPING_CHARGES:'ItemShipping'],

        // Party
        partyTypeId:[PERSON:'PtyPerson', PARTY_GROUP:'PtyOrganization'],
        roleTypeId:['_NA_':null, ACCOUNT:'Account', ACCOUNT_LEAD:'AccountLead', LEAD:'AccountLead', AGENT:'Agent',
                AFFILIATE:'Affiliate', CARRIER:'Carrier', COMPETITOR:'Competitor', CONSUMER:'Consumer', CONTRACTOR:'Contractor',
                DISTRIBUTOR:'Distributor', ISP:'InternetSp', HOSTING_SERVER:'HostingProvider', MANUFACTURER:'Manufacturer',
                OWNER:'Owner', PARTNER:'Partner', PROSPECT:'Prospect', REFERRER:'Referrer', SHAREHOLDER:'Shareholder',
                SUBSCRIBER:'Subscriber', SUPPLIER:'Supplier', VISITOR:'Visitor', WEB_MASTER:'WebMaster',
                ACCOUNTANT:'Accountant', ADMIN:'Administrator', BUYER:'Buyer', CASHIER:'Cashier', CLIENT_BILLING:'ClientBilling',
                CLIENT_MANAGER:'ClientManager', CONTACT:'Contact', EMAIL_ADMIN:'EmailAdmin', EMPLOYEE:'Employee',
                FAMILY_MEMBER:'FamilyMember', MANAGER:'Manager', ORDER_CLERK:'OrderClerk', PACKER:'Packer',
                PICKER:'Picker', RECEIVER:'Receiver', SALES_REP:'SalesRepresentative', SHIPMENT_CLERK:'ShipmentClerk',
                SPONSOR:'Sponsor', SPOUSE:'Spouse', STOCKER:'Stocker', SUPPLIER_AGENT:'SupplierAgent', WORKER:'Worker',
                ASSOCIATION:'OrgAssociation', DEPARTMENT:'OrgDepartment', DIVISION:'OrgDivision', HOUSEHOLD:'OrgHousehold',
                INTERNAL_ORGANIZATIO:'OrgInternal', PROJECT_TEAM:'OrgTeam', SCRUM_TEAM:'OrgTeam', ORGANIZATION_UNIT:'OrgUnit',
                PARENT_ORGANIZATION:'OrgParent', REGULATORY_AGENCY:'OrgRegulatoryAgency', SUBSIDIARY:'OrgSubsidiary',
                TAX_AUTHORITY:'OrgTaxAuthority', UNION:'OrgUnion',
                CUSTOMER:'Customer', BILL_TO_CUSTOMER:'CustomerBillTo', END_USER_CUSTOMER:'CustomerEndUser',
                PLACING_CUSTOMER:'CustomerPlacing', SHIP_TO_CUSTOMER:'CustomerShipTo', VENDOR:'Vendor',
                BILL_FROM_VENDOR:'VendorBillFrom', SHIP_FROM_VENDOR:'VendorShipFrom'],
        partyRelationshipTypeId:[ACCOUNT:'PrtCustomer', AGENT:'PrtAgent', CHILD:'PrtChild', CONTACT_REL:'PrtContact',
                CUSTOMER_REL:'PrtCustomer', EMPLOYMENT:'PrtEmployee', FRIEND:'PrtFriend', GROUP_ROLLUP:'PrtOrgRollup',
                LEAD_OWNER:'PrtCustomer', MANAGER:'PrtManager', SALES_AFFILIATE:'PrtSalesAffiliate', SALES_REP:'PrtRepresentative',
                SPOUSE:'PrtSpouse', SUPPLIER_REL:'PrtSupplier'],

        // Payment
        paymentTypeId:[COMMISSION_PAYMENT:'PtInvoicePayment', CUSTOMER_DEPOSIT:'PtPrePayment', CUSTOMER_PAYMENT:'PtInvoicePayment',
                CUSTOMER_REFUND:'PtRefund', DISBURSEMENT:'PtDisbursement', SALES_TAX_PAYMENT:'PtInvoicePayment',
                TAX_PAYMENT:'PtInvoicePayment', VENDOR_PAYMENT:'PtInvoicePayment', VENDOR_PREPAY:'PtPrePayment'],
                // ADJUSTMENT, GC_DEPOSIT, GC_WITHDRAWAL, INCOME_TAX_PAYMENT, INTEREST_RECEIPT, PAY_CHECK, PAYROLL_TAX_PAYMENT, PAYROL_PAYMENT, POS_PAID_IN, POS_PAID_OUT, RECEIPT, SALES_TAX_PAYMENT, TAX_PAYMENT, VENDOR_PAYMENT, VENDOR_PREPAY
        // paymentInstrumentEnumId and paymentMethodTypeEnumId are based on paymentMethodTypeId
        paymentInstrumentEnumId:[CASH:'PiCash', CERTIFIED_CHECK:'PiCertifiedCheck', COMPANY_ACCOUNT:'PiCompanyAccount',
                COMPANY_CHECK:'PiCompanyCheck', CREDIT_CARD:'PiCreditCard', EFT_ACCOUNT:'PiAch', EXT_BILLACT:'PiBillingAccount',
                EXT_COD:'PiCod', EXT_OFFLINE:'PiCash', FIN_ACCOUNT:'PiFinancialAccount', GIFT_CARD:'PiGiftCard',
                GIFT_CERTIFICATE:'PiGiftCerificate', MONEY_ORDER:'PiMoneyOrder', PERSONAL_CHECK:'PiPersonalCheck', PETTY_CASH:'PiCash'],
                // ADJUSTMENT, EXT_AMAZON, EXT_AUTHORIZE_NET, EXT_EBAY, EXT_IDEAL, EXT_PAYPAL, EXT_WORLDPAY,
        paymentMethodTypeEnumId:[CERTIFIED_CHECK:'PmtBankAccount', COMPANY_CHECK:'PmtBankAccount', EFT_ACCOUNT:'PmtBankAccount',
                CREDIT_CARD:'PmtCreditCard', FIN_ACCOUNT:'PmtFinancialAccount', GIFT_CARD:'PmtGiftCard', PERSONAL_CHECK:'PmtBankAccount'],
                // ADJUSTMENT,CASH,COMPANY_ACCOUNT,EXT_AMAZON,EXT_AUTHORIZE_NET,EXT_BILLACT,EXT_COD,EXT_EBAY,EXT_IDEAL,EXT_OFFLINE,EXT_PAYPAL,EXT_WORLDPAY,GIFT_CERTIFICATE,MONEY_ORDER,PETTY_CASH
        cardType:[AmericanExpress:'CctAmericanExpress', Discover:'CctDiscover', MasterCard:'CctMastercard', Visa:'CctVisa'],
        paymentStatusId:[PAYMENT_AUTHORIZED:'PmntAuthorized', PAYMENT_CANCELLED:'PmntCancelled', PAYMENT_DECLINED:'PmntDeclined',
                PAYMENT_NOT_AUTH:'PmntPromised', PAYMENT_NOT_RECEIVED:'PmntPromised', PAYMENT_RECEIVED:'PmntDelivered',
                PAYMENT_REFUNDED:'PmntRefunded', PAYMENT_SETTLED:'PmntDelivered',
                PMNT_CANCELLED:'PmntCancelled', PMNT_CONFIRMED:'PmntConfirmed', PMNT_NOT_PAID:'PmntPromised',
                PMNT_RECEIVED:'PmntDelivered', PMNT_SENT:'PmntDelivered', PMNT_VOID:'PmntVoid'],
        paymentServiceTypeEnumId:[PRDS_PAY_AUTH:'PgoAuthorize', PRDS_PAY_AUTH_VERIFY:'PgoValidateAuthorize',
                PRDS_PAY_CAPTURE:'PgoCapture', PRDS_PAY_CREDIT:'PgoAuthAndCapture', PRDS_PAY_REAUTH:'PgoAuthorize',
                PRDS_PAY_REFUND:'PgoRefund', PRDS_PAY_RELEASE:'PgoRelease'],


        // Product
        // productTypeEnumId, assetTypeEnumId, and assetClassEnumId based on Product.productTypeId
        productTypeEnumId:[ASSET_USAGE:'PtAssetUse', ASSET_USAGE_OUT_IN:'PtAssetUse', DIGITAL_GOOD:'PtDigital',
                FINDIG_GOOD:'PtDigitalAsset', FINISHED_GOOD:'PtAsset', GOOD:'PtAsset', RAW_MATERIAL:'PtAsset', SERVICE:'PtService',
                SERVICE_PRODUCT:'PtService', SUBASSEMBLY:'PtAsset', WIP:'PtAsset'],
        assetTypeEnumId:[FINDIG_GOOD:'AstTpInventory', FINISHED_GOOD:'AstTpInventory', GOOD:'AstTpInventory',
                RAW_MATERIAL:'AstTpInventory', SERVICE_PRODUCT:'AstTpInventory', SUBASSEMBLY:'AstTpInventory', WIP:'AstTpInventory'],
        assetClassEnumId:[FINDIG_GOOD:'AsClsInventoryFin', FINISHED_GOOD:'AsClsInventoryFin', GOOD:'AsClsInventoryFin',
                RAW_MATERIAL:'AsClsInventoryRaw', SERVICE_PRODUCT:'AsClsInventoryFin', SUBASSEMBLY:'AsClsInventorySub', WIP:'AsClsInventorySub'],
        productPriceTypeId:[DEFAULT_PRICE:'PptCurrent', LIST_PRICE:'PptList'],

        // Inventory
        inventoryStatusId:[INV_ACTIVATED:'AstActivated', INV_AVAILABLE:'AstAvailable', INV_BEING_TRANSFERED:'AstInTransfer',
                INV_BEING_TRANS_PRM:'AstInTransferPromise', INV_DEACTIVATED:'AstDeactivated', INV_DEFECTIVE:'AstDefective',
                INV_DELIVERED:'AstDelivered', INV_NS_DEFECTIVE:'AstDefective', INV_NS_ON_HOLD:'AstOnHold', INV_NS_RETURNED:'AstReturned'],
        varianceReasonEnumId:[VAR_DAMAGED:'InVrDamaged', VAR_FOUND:'InVrFound', VAR_INTEGR:'InVrIntegration', VAR_LOST:'InVrLost',
                VAR_MISSHIP_ORDERED:'InVrMisShipOrdered', VAR_MISSHIP_SHIPPED:'InVrMisShipShipped', VAR_SAMPLE:'InVrSample', VAR_STOLEN:'InVrStolen'],
        rejectionId:[SRJ_DAMAGED:'ArjDamaged', SRJ_LOST_INTRANS:'ArjLost', SRJ_NOT_ORDERED:'ArjNotOrdered',
                SRJ_NOT_SPEC:'ArjNotToSpec', SRJ_OVER_SHIPPED:'ArjOverShipped'],

        // Shipment
        shipmentTypeId:[DROP_SHIPMENT:'ShpTpDrop', INCOMING_SHIPMENT:'ShpTpIncoming', OUTGOING_SHIPMENT:'ShpTpOutgoing',
                PURCHASE_RETURN:'ShpTpPurchaseReturn', PURCHASE_SHIPMENT:'ShpTpPurchase', SALES_RETURN:'ShpTpSalesReturn',
                SALES_SHIPMENT:'ShpTpSales', TRANSFER:'ShpTpTransfer'],
        shipmentStatusId:[SHIPMENT_CANCELLED:'ShipCancelled', SHIPMENT_DELIVERED:'ShipDelivered', SHIPMENT_INPUT:'ShipInput',
                SHIPMENT_PACKED:'ShipPacked', SHIPMENT_PICKED:'ShipPicked', SHIPMENT_SCHEDULED:'ShipScheduled',
                SHIPMENT_SHIPPED:'ShipShipped',
                PURCH_SHIP_RECEIVED:'ShipDelivered', PURCH_SHIP_SHIPPED:'ShipShipped', PURCH_SHIP_CREATED:'ShipInput'],
        carrierServiceStatusId:[SHRSCS_ACCEPTED:'ShrssAccepted', SHRSCS_CONFIRMED:'ShrssConfirmed',
                SHRSCS_NOT_STARTED:'ShrssNotStarted', SHRSCS_VOIDED:'ShrssVoided']
    ]
}

/*



RETURN_ACCEPTED:'', RETURN_CANCELLED:'', RETURN_COMPLETED:'', RETURN_MAN_REFUND:'', RETURN_RECEIVED:'', RETURN_REQUESTED:'',

SUP_RETURN_ACCEPTED:'', SUP_RETURN_CANCELLED:'', SUP_RETURN_COMPLETED:'', SUP_RETURN_REQUESTED:'', SUP_RETURN_SHIPPED:'',

 */
