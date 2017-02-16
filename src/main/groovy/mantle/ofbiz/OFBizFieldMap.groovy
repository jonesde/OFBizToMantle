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

        paymentMethodTypeId:[CERTIFIED_CHECK:'PmtBankAccount', COMPANY_CHECK:'PmtBankAccount', EFT_ACCOUNT:'PmtBankAccount',
                CREDIT_CARD:'PmtCreditCard', FIN_ACCOUNT:'PmtFinancialAccount', GIFT_CARD:'PmtGiftCard', PERSONAL_CHECK:'PmtBankAccount'],
            // ADJUSTMENT,CASH,COMPANY_ACCOUNT,EXT_AMAZON,EXT_AUTHORIZE_NET,EXT_BILLACT,EXT_COD,EXT_EBAY,EXT_IDEAL,EXT_OFFLINE,EXT_PAYPAL,EXT_WORLDPAY,GIFT_CERTIFICATE,MONEY_ORDER,PETTY_CASH
        cardType:[AmericanExpress:'CctAmericanExpress', Discover:'CctDiscover', MasterCard:'CctMastercard', Visa:'CctVisa'],

        // these 3 based on Product.productTypeId
        productTypeEnumId:[ASSET_USAGE:'PtAssetUse', ASSET_USAGE_OUT_IN:'PtAssetUse', DIGITAL_GOOD:'PtDigital',
                FINDIG_GOOD:'PtDigitalAsset', FINISHED_GOOD:'PtAsset', GOOD:'PtAsset', RAW_MATERIAL:'PtAsset', SERVICE:'PtService',
                SERVICE_PRODUCT:'PtService', SUBASSEMBLY:'PtAsset', WIP:'PtAsset'],
        assetTypeEnumId:[FINDIG_GOOD:'AstTpInventory', FINISHED_GOOD:'AstTpInventory', GOOD:'AstTpInventory',
                RAW_MATERIAL:'AstTpInventory', SERVICE_PRODUCT:'AstTpInventory', SUBASSEMBLY:'AstTpInventory', WIP:'AstTpInventory'],
        assetClassEnumId:[FINDIG_GOOD:'AsClsInventoryFin', FINISHED_GOOD:'AsClsInventoryFin', GOOD:'AsClsInventoryFin',
                RAW_MATERIAL:'AsClsInventoryRaw', SERVICE_PRODUCT:'AsClsInventoryFin', SUBASSEMBLY:'AsClsInventorySub', WIP:'AsClsInventorySub'],
        productPriceTypeId:[DEFAULT_PRICE:'PptCurrent', LIST_PRICE:'PptList']
    ]
}

/*

 */