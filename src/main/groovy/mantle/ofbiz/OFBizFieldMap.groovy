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
        // AcctgTrans
        acctgTransTypeId:[AMORTIZATION:'AttAmortization', AR_CLOSING_ADJ:'AttPeriodClosing', AR_PMT_CREDIT:'AttInvoiceAdjust',
                CAPITALIZATION:'AttCapitalization', CREDIT_LINE:'AttCreditLine', CREDIT_MEMO:'AttCreditMemo',
                CUST_RTN_INVOICE:'AttCustRtnInvoice', DEPRECIATION:'AttDepreciation', DISBURSEMENT:'AttDisbursement',
                EXTERNAL_ACCTG_TRANS:'AttExternal', INCOMING_PAYMENT:'AttIncomingPayment', INTERNAL_ACCTG_TRANS:'AttInternal',
                INVENTORY:'AttSalesInventory', INVENTORY_RETURN:'AttReturnInventory', ITEM_VARIANCE:'AttInventoryVariance',
                MANUFACTURING:'AttManufacturing', NOTE:'AttNote', OBLIGATION_ACCTG_TRA:'AttObligation',
                OTHER_INTERNAL:'AttInternal', OTHER_OBLIGATION:'AttOtherObligation', OUTGOING_PAYMENT:'AttOutgoingPayment',
                PAYMENT_ACCTG_TRANS:'AttPayment', PAYMENT_APPL:'AttPaymentApplied', PERIOD_CLOSING:'AttPeriodClosing',
                PURCHASE_INVOICE:'AttPurchaseInvoice', RECEIPT:'AttReceipt', SALES:'AttSales', SALES_INVOICE:'AttSalesInvoice',
                SALES_SHIPMENT:'AttInventoryIssuance', SHIPMENT_RECEIPT:'AttInventoryReceipt', TAX_DUE:'AttTaxDue'],
        glAccountTypeId:[ACCOUNTS_PAYABLE:'GatAccountsPayable', ACCOUNTS_RECEIVABLE:'GatAccountsReceivable',
                ACCPAYABLE_UNAPPLIED:'GatAccPayableUnapplied', ACCREC_UNAPPLIED:'GatAccReceivableUnapplied',
                AR_TRADE:'GatAccountsReceivable', COGS_ACCOUNT:'GatCogs', COMMISSIONS_PAYABLE:'GatCommissionsAccrued',
                CURRENT_ASSET:'GatCurrentAsset', CURRENT_LIABILITY:'GatCurrentLiability', CUSTOMER_CREDIT:'GatCustomerCredits',
                CUSTOMER_DEPOSIT:'GatCustomerDeposits', FIXED_ASSET:'GatFixedAsset', INVENTORY_ACCOUNT:'GatInventory',
                INVENTORY_XFER_IN:'GatPayInventoryTranIn', OWNERS_DRAW:'GatRetainedEarnings', OWNERS_EQUITY:'GatOwnersEquity',
                PAYMENT_AR_CREDIT:'GatAccountsReceivable', PREPAID_EXPENSES:'GatPrepaidExpenses', PROFIT_LOSS_ACCOUNT:'GatProfitLoss',
                RAWMAT_INVENTORY:'GatInventory', RETAINED_EARNINGS:'GatRetainedEarnings', UNINVOICED_SHIP_RCPT:'GatUninvoicedShpmntRcpts'],
                // NOTE: these are just the major, and not all, GL account types

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

        // FinancialAccount
        finAccountTypeId:[BANK_ACCOUNT:'DepositAccount', CREDIT_CARD_ACCOUNT:'LoanAccount', DEPOSIT_ACCOUNT:'DepositAccount',
                EQUITY_LINE_ACCOUNT:'LoanAccount', GIFTCERT_ACCOUNT:'GiftCertificate', INVESTMENT_ACCOUNT:'DepositAccount',
                LOAN_ACCOUNT:'LoanAccount', REPLENISH_ACCOUNT:'Replenish', STORE_CREDIT_ACCT:'CustomerCredit', SVCCRED_ACCOUNT:'ServiceCredit'],
        finAccountStatusId:[FNACT_ACTIVE:'FaActive', FNACT_CANCELLED:'FaCancelled', FNACT_MANFROZEN:'FaManFrozen', FNACT_NEGPENDREPL:'FaNegPendRepl'],
        finAccountTransTypeId:[ADJUSTMENT:'FattAdjustment', DEPOSIT:'FattDeposit', WITHDRAWAL:'FattWithdraw'],

        // Invoice
        invoiceTypeId:[COMMISSION_INVOICE:'InvoiceCommission', CUST_RTN_INVOICE:'InvoiceReturn', PAYROL_INVOICE:'InvoicePayroll',
                PURCHASE_INVOICE:'InvoiceSales', PURC_RTN_INVOICE:'InvoiceReturn', PUR_INV_TEMPLATE:'InvoiceTemplate',
                SALES_INVOICE:'InvoiceSales', SALES_INV_TEMPLATE:'InvoiceTemplate', TEMPLATE:'InvoiceTemplate',
                VENDOR_CREDIT_MEMO:'InvoiceCreditMemo', INTEREST_INVOICE:'InvoiceSales'],
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
        // NOTE: ORDER_APPROVED and ORDER_PROCESSING are mapped to OrderPlaced for in flight orders to review, approve and ship after migration
        orderStatusId:[ORDER_APPROVED:'OrderPlaced', ORDER_CANCELLED:'OrderCancelled', ORDER_COMPLETED:'OrderCompleted',
                ORDER_CREATED:'OrderOpen', ORDER_HOLD:'OrderHold', ORDER_PROCESSING:'OrderPlaced',
                ORDER_REJECTED:'OrderRejected', ORDER_SENT:'OrderSent'],
        shipmentMethodTypeId:[AIR:'ShMthNextDay', GROUND:'ShMthGround', NO_SHIPPING:null, STANDARD:'ShMthGround', 'USPS-CAN':'ShMthGround', 'USPS Free':'ShMthGround'],
        orderItemTypeId:[ASSET_ORDER_ITEM:'ItemAsset', BULK_ORDER_ITEM:'ItemInventory', INVENTORY_ORDER_ITEM:'ItemInventory',
                PRODUCT_ORDER_ITEM:'ItemProduct', PURCHASE_SPECIFIC:'ItemInventory', RENTAL_ORDER_ITEM:'ItemRental',
                SUPPLIES_ORDER_ITEM:'ItemExpOfficeSup', WORK_ORDER_ITEM:'ItemWorkEffort', ORD_SHRINKAGE_ADJ:'ItemShrinkage'],
        orderAdjustmentTypeId:[ADDITIONAL_FEATURE:'ItemAddtlFeature', DISCOUNT_ADJUSTMENT:'ItemDiscount', FEE:'ItemFee',
                MISCELLANEOUS_CHARGE:'ItemMiscCharge', PROMOTION_ADJUSTMENT:'ItemDiscount', REPLACE_ADJUSTMENT:'ItemReplacement',
                SALES_TAX:'ItemSalesTax', SHIPPING_CHARGES:'ItemShipping', ORD_SHRINKAGE_ADJ:'ItemShrinkage',
                ORD_CHARGEBACK_ADJ:'ItemChargebackAdjust', ORD_COMPLIANCE_ADJ:'ItemComplianceFee'],

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
                TAX_PAYMENT:'PtInvoicePayment', VENDOR_PAYMENT:'PtInvoicePayment', VENDOR_PREPAY:'PtPrePayment',
                ADJUSTMENT:'PtInvoicePayment', RECEIPT:'PtInvoicePayment', GC_DEPOSIT:'PtFinancialAccount', GC_WITHDRAWAL:'PtFinancialAccount',
                INCOME_TAX_PAYMENT:'PtInvoicePayment', INTEREST_RECEIPT:'PtInvoicePayment', PAY_CHECK:'PtInvoicePayment',
                PAYROLL_TAX_PAYMENT:'PtInvoicePayment', PAYROL_PAYMENT:'PtInvoicePayment'],
                // POS_PAID_IN, POS_PAID_OUT
        // paymentInstrumentEnumId and paymentMethodTypeEnumId are based on paymentMethodTypeId
        paymentInstrumentEnumId:[CASH:'PiCash', CERTIFIED_CHECK:'PiCertifiedCheck', COMPANY_ACCOUNT:'PiCompanyAccount',
                COMPANY_CHECK:'PiCompanyCheck', CREDIT_CARD:'PiCreditCard', EFT_ACCOUNT:'PiAch', EXT_BILLACT:'PiBillingAccount',
                EXT_COD:'PiCod', EXT_OFFLINE:'PiCash', EXT_PAYPAL:'PiPayPalAccount', FIN_ACCOUNT:'PiFinancialAccount', GIFT_CARD:'PiGiftCard',
                GIFT_CERTIFICATE:'PiGiftCertificate', MONEY_ORDER:'PiMoneyOrder', PERSONAL_CHECK:'PiPersonalCheck',
                PETTY_CASH:'PiCash', EXT_AMAZON:'PiCreditCard', EXT_AUTHORIZE_NET:'PiCreditCard', ADJUSTMENT:'PiOther'],
                // EXT_EBAY, EXT_IDEAL, EXT_WORLDPAY
        paymentMethodTypeEnumId:[CERTIFIED_CHECK:'PmtBankAccount', COMPANY_CHECK:'PmtBankAccount', EFT_ACCOUNT:'PmtBankAccount',
                CREDIT_CARD:'PmtCreditCard', FIN_ACCOUNT:'PmtFinancialAccount', GIFT_CARD:'PmtGiftCard', PERSONAL_CHECK:'PmtBankAccount',
                EXT_PAYPAL:'PmtPayPalAccount', PETTY_CASH:'PmtOther', CASH:'PmtOther', ADJUSTMENT:'PmtOther', EXT_AUTHORIZE_NET:'PmtOther'],
                // COMPANY_ACCOUNT,EXT_AMAZON,EXT_BILLACT,EXT_COD,EXT_EBAY,EXT_IDEAL,EXT_OFFLINE,EXT_WORLDPAY,GIFT_CERTIFICATE,MONEY_ORDER
        cardType:[AmericanExpress:'CctAmericanExpress', Discover:'CctDiscover', MasterCard:'CctMastercard', Visa:'CctVisa'],
        paymentStatusId:[PAYMENT_AUTHORIZED:'PmntAuthorized', PAYMENT_CANCELLED:'PmntCancelled', PAYMENT_DECLINED:'PmntDeclined',
                PAYMENT_NOT_AUTH:'PmntPromised', PAYMENT_NOT_RECEIVED:'PmntPromised', PAYMENT_RECEIVED:'PmntDelivered',
                PAYMENT_REFUNDED:'PmntDelivered', PAYMENT_SETTLED:'PmntDelivered',
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
                SHRSCS_NOT_STARTED:'ShrssNotStarted', SHRSCS_VOIDED:'ShrssVoided'],

        // GL Account Mappings - for default OFBiz and Mantle chart of accounts
        // this doesn't have all accounts, just actually used ones from initial test case; add or modify as needed
        // in addition to the standard OFBiz GL accounts there are various additional ones that fill in common gaps in the default
        //     OFBiz chart of accounts; see account names and mappings for any accounts you may have added for the much more
        //     comprehensive set of default accounts in Mantle
        glAccountId:[
            '111100':'111100000', // GENERAL CHECKING ACCOUNT
            '111200':'111200000', // PAYROLL CHECKING ACCOUNT
            '111300':'111300000', // SINGLE TITLE ORDER PLAN (STOP) CHECKING (OTHER CHECKING ACCOUNT)
            '111400':'111400000', // MONEY MARKET
            '111500':'111500000', // CERTIFICATES OF DEPOSIT
            '111600':'111600000', // SAVINGS ACCOUNTS
            '111700':'111700000', // CASH IN REGISTERS
            '111800':'111800000', // CASH FOR OUT OF STORE EVENTS
            '111900':'111900000', // PETTY CASH
            '112000':'112000000', // UNDEPOSITED RECEIPTS

            '120000':'121000000', // ACCOUNTS RECEIVABLE
            '121000':'121100000', // ACCOUNTS RECEIVABLE - TRADE
            '121100':'121100000', // ACCOUNTS RECEIVABLE - TRADE
            '121200':'121200000', // ACCOUNTS RECEIVABLE - NON-SUFFICIENT FUNDS RETURNED CHECKS
            '121800':'121500000', // ACCOUNTS RECEIVABLE TRADE - INTEREST RECEIVABLE
            '121900':'121900000', // ACCOUNTS RECEIVABLE TRADE - ALLOWANCE FOR BAD DEBT
            '122000':'122000000', // IN TRANSIT FROM CREDIT CARD PROCESSORS
            '122100':'122000000', '122200':'122000000', '122300':'122000000', '122400':'122000000', '122500':'122000000',
            '122900':'122900000', // ACCOUNTS RECEIVABLE CREDIT CARDS - ALLOWANCE FOR BAD DEBT
            '123000':'123000000', // VENDOR ADVERTISING ALLOWANCES RECEIVABLE
            '123101':'121900000', // ALLOWANCE FOR BAD DEBTS
            '124000':'124000000', // LOANS RECEIVABLE
            '124100':'124100000', // LOANS RECEIVABLE - EMPLOYEES
            '124101':'124110000', '124102':'124120000', '124103':'124130000', '124104':'124140000', '124105':'124150000', '124106':'124160000', '124108':'124180000',
            '124200':'124200000', // LOANS RECEIVABLE - OWNERS
            '124201':'124210000','124202':'124220000',
            '124300':'124300000', // LOANS RECEIVABLE - OTHERS
            '125000':'125000000', // RECEIVABLE FROM INVENTORY TRANSFERRED OUT
            '126000':'126000000', // ACCOUNTS RECEIVABLE - UNAPPLIED PAYMENTS
            '129000':'129000000', // A/R MISCELLANEOUS

            '140000':'140000000', // INVENTORY
            '141000':'141100000', // RAW MATERIALS INVENTORY
            '142000':'141200000', // WORK IN PROGRESS INVENTORY
            '142001':'141300000', // FINISHED GOOD INVENTORY
            '142002':'141400000', // PACKAGING COMPONENTS INVENTORY
            '142003':'149000000', // INVENTORY - PREPAID => UNRECEIVED INVENTORY
            '149000':'148000000', // INVENTORY - AVERAGE COST VALUATION ADJUSTMENT

            '150000':'150000000', // PREPAID EXPENSES, DEPOSITS, OTHER CURRENT ASSETS
            '151000':'151000000', // PREPAID EXPENSES
            '151100':'151100000', // PREPAID INSURANCE
            '151200':'151200000', // PREPAID RENT
            '151300':'151300000', // PREPAID INTEREST
            '151900':'151900000', // PREPAID OTHER
            '152000':'152000000', // DEPOSITS
            '152100':'152100000', // DEPOSIT - UPS
            '152200':'152200000', // DEPOSIT - UTILITIES
            '152300':'152300000', // DEPOSIT - RENT
            '152900':'152900000', // DEPOSITS - OTHER
            '153000':'153000000', // OTHER CURRENT ASSETS

            '160000':'132000000', // LAND AND BUILDING
            '161000':'132100000', // LAND
            '162000':'132200000', // BUILDING
            '163000':'132300000', // LAND IMPROVEMENTS
            '164000':'132400000', // BUILDING IMPROVEMENTS
            '165000':'132500000', // LEASEHOLD IMPROVEMENTS

            '170000':'131000000', // EQUIPMENT
            '171000':'131100000', // OFFICE FURNITURE - FIXTURES
            '171001':'131500000', // PLANT EQUIPMENT
            '172000':'131100000', // OFFICE EQUIPMENT
            '173000':'131300000', // DATA PROCESSING SOFTWARE
            '174000':'131400000', // VEHICLES

            '180000':'180000000', // ACCUMULATED DEPRECIATION
            '183000':'181400000', // ACCUMULATED DEPRECIATION - BUILDING IMPROVEMENTS
            '184000':'181500000', // ACCUM. DEPRECIATION - LEASEHOLD IMPROVEMENTS
            '185000':'182500000', // ACCUM. DEPRECIATION - PLANT EQUIPMENT
            '185001':'182100000', // ACCUM. DEPRECIATION - OFFICE FURNITURE
            '185002':'182100000', // ACCUM. DEPRECIATION - OFFICE EQUIPMENT
            '188000':'182400000', // ACCUM. DEPRECIATION - VEHICLES

            '190000':'134900000', // OTHER ASSETS
            '191000':'133000000', // INTANGIBLE ASSETS
            '191100':'133100000', // COVENANT NOT TO COMPETE-PREVIOUS OWNER
            '191800':'133400000', '191900':'138400000',
            '191200':'133200000', // GOODWILL
            '191300':'133300000', // CUSTOMER LISTS
            '192000':'134100000', // CASH SURRENDER VALUE OF LIFE INSURANCE
            '199000':'134900000', // OTHER ASSETS
            '199001':'911000000', // TRANSFER CLEARING - ASSET

            '210000':'212000000', '210001':'212000000', // ACCOUNTS PAYABLE
            '211000':'211000000', // ACCOUNTS PAYABLE - MERCHANDISE
            '212000':'212000000', // ACCOUNTS PAYABLE - OPERATING
            '212004':'212540000', '212005':'212550000', // LOAN PAYABLE
            '213000':'251000000', // CUSTOMER CREDIT AND DEPOSITS
            '213100':'251100000', // MERCHANDISE CREDITS UNREDEEMED
            '213200':'251200000', // GIFT CERTIFICATES UNREDEEMED
            '213300':'251300000', // SPECIAL ORDER DEPOSITS
            '213400':'251400000', // MAIL ORDER DEPOSITS
            '213500':'251500000', // CUSTOMER DEPOSIT ACCOUNT
            '214000':'214000000', // UNINVOICED ITEM RECEIPTS
            '215000':'215000000', // PAYABLE FOR INVENTORY TRANSFERRED IN
            '216000':'216000000', // ACCOUNTS PAYABLE - UNAPPLIED PAYMENTS

            '220000':'220000000', // ACCRUED EXPENSES
            '221000':'221000000', // ACCRUED PAYROLL
            '221100':'221100000', // ACCRUED COMMISSIONS DUE
            '221200':'221200000', // ACCRUED WAGES DUE
            '221300':'221300000', // ACCRUED EMPLOYEE MEDICAL PREMIUMS
            '222000':'222000000', // PAYROLL WITHHOLDINGS
            '222100':'222100000', // FEDERAL WITHHOLDING
            '222200':'222110000', // FICA WITHHOLDING
            '222400':'222400000', // STATE WITHHOLDING
            '222500':'222500000', // LOCAL WITHHOLDING
            '222600':'222600000', // 401k - PENSION WITHHOLDING
            '222700':'222700000', // GARNISHMENT WITHHOLDING
            '222800':'222800000', // EMPLOYEE BENEFITS WITHHOLDING
            '222900':'222900000', // MISCELLANEOUS WITHHOLDING
            '223000':'223000000', // ACCRUED PAYROLL TAXES
            '223100':'223100000', // ACCRUED PAYROLL TAXES - FICA EMPLOYER'S SHARE
            '223200':'223200000', // ACCRUED PAYROLL TAXES - MEDICARE EMPLOYER'S SHARE
            '223300':'223300000', // ACCRUED PAYROLL TAXES - FEDERAL UNEMPLOYMENT TAX
            '223400':'223400000', // ACCRUED PAYROLL TAXES - STATE UNEMPLOYMENT TAX
            '223500':'223500000', // 401k - PENSION EMPLOYER CONTRIBUTION
            '224000':'224000000', // SALES TAX COLLECTED
            '224100':'224000000', // SALES TAX COLLECTED USA
            '224153':'224000000', // SALES TAX COLLECTED USA UT
            '225000':'225000000', // ACCRUED USE TAX
            '226000':'226000000', // ACCRUED RETIREMENT PLAN EXPENSE
            '229000':'229000000', // OTHER ACCRUED EXPENSES

            '230000':'230000000', // CURRENT NOTES PAYABLE
            '231000':'231000000', '231001':'231010000', // SHORT TERM OBLIGATION
            '239000':'239000000', // CURRENT PORTION OF LONG-TERM DEBT

            '240000':'240000000', // LONG-TERM NOTES PAYABLE
            '242000':'242000000', // MORTGAGE NOTE PAYABLE
            '243000':'243000000', // LOAN FROM OWNER OR STOCKHOLDER
            /* TODO: below are custom mappings, these are better defaults:
            '241000':'241000000', // LONG-TERM OBLIGATION
            '248000':'248000000', // OTHER LONG TERM OBLIGATION
            */
            '241000':'241100000', // LONG-TERM OBLIGATION
            '248000':'241200000', // OTHER LONG TERM OBLIGATION

            // these mappings may vary based on how OFBiz was used and how company is organized (these are for mapping to S CORP)
            '300000':'330000000', // OWNERS EQUITY
            '310000':'331100000', // CAPITAL STOCK (Common Stock)
            '330000':'335000000', // RETAINED EARNINGS
            '331000':'332100000', // PAID-IN CAPITAL (Paid-in Capital in Excess of Par on Common Stock)
            '332000':'331300000', // TREASURY STOCK
            '336000':'850000000', // CURRENT PERIOD PROFIT (LOSS)
            '342000':'334000000', // OWNER DRAWS

            '400000':'400000000', // SALES
            '401000':'410000000', // SALES REVENUE
            '401001':'411000000', '401002':'411000000', '401003':'411000000', '401004':'411000000', '401005':'411000000', // consolidate to Product Sales
            '409000':'419000000', // MISCELLANEOUS SALES
            '410000':'520000000', // DISCOUNTS AND WRITE DOWNS
            '410001':'522000000', // SALES DISCOUNTS AND ALLOWANCES
            '410003':'522100000', // COUPONS AND CREDITS
            '410005':'522200000', // PROMOTIONAL DISCOUNTS
            '410006':'523100000', '410007':'523100000', '410008':'523100000', // SLOTTING ALLOWANCES (FEES)
            '410009':'525100000', // FREE FILLS
            '410010':'525200000', // BROKER (FREE) SAMPLES
            '410011':'521000000', // CASH DISCOUNTS (PROMPT PAYMENT)
            '420000':'420000000', // CUSTOMER RETURNS
            '421000':'421000000', // CUSTOMER RETURNS - PRODUCTS
            '422000':'422000000', // CUSTOMER RETURNS - PROMOTIONS
            '423000':'423000000', // CUSTOMER RETURNS - ADJUSTMENTS
            '424000':'424000000', // CUSTOMER RETURNS - WORK OR TIME ENTRY

            '500000':'510000000', // COST OF GOODS SOLD
            '502000':'511000000', // MERCHANDISE PURCHASES
            '502001':'511000000', '502002':'511000000', '502003':'511000000', '502004':'511000000', '502005':'511000000', // consolidate
            '502006':'516000000', // ROYALTIES ON GOODS
            '502007':'516010000', '502008':'516020000', '502009':'516030000', '502010':'516040000', '502011':'516050000',
            '502012':'794010000', // exception, non-cogs royalty
            '510000':'519000000', // FREIGHT
            '510001':'519100000', // FREIGHT IN
            '510201':'519200000', // FREIGHT OUT
            '511000':'521000000', // PROMPT PAYMENT DISCOUNTS
            '512000':'530000000', // RETURNS EXPENSE
            '512001':'525200000', // SAMPLES (Sales Sample Expense)
            '512050':'531000000', // RETURNS PENALTIES
            '513000':'532000000', // RETURNS TO VENDORS
            '514000':'527000000', // INVENTORY SHRINKAGE
            '515000':'525000000', // WRITE DOWNS BELOW COST
            '516100':'786100000', // PURCHASE ORDER ADJUSTMENTS
            '517100':'782100000', // ACCOUNTS RECEIVABLE WRITE OFF
            '517200':'862000000', // ACCOUNTS PAYABLE WRITE OFF
            '517300':'863000000', // COMMISSIONS PAYABLE WRITE OFF
            '517400':'782400000', // INTEREST INCOME WRITE OFF
            '518100':'817000000', // FOREIGN EXCHANGE GAIN
            '518200':'797000000', // FOREIGN EXCHANGE LOSS
            '519000':'526000000', // INVENTORY ADJUSTMENT
            '519201':'527100000', // OBSOLETE INVENTORY (under Inventory Shrinkage)

            '601000':'631000000', // WAGES AND COMMISSIONS
            '601100':'631100000', // WAGES - EMPLOYEE
            '601101':'631400000', // WAGES - OFFICERS
            '601200':'631200000', // WAGES - OWNERS
            '601400':'631700000', '601401':'631710000', '601403':'631730000', '601404':'631740000', // SALES COMMISSIONS - NON-EMPLOYEE
            '601405':'661000000', // OUTSIDE LABOR (Outside Labor and Services)
            '602100':'632100000', // VACATION PAY - EMPLOYEES
            '602200':'632200000', // VACATION PAY - OWNERS
            '603100':'632300000', // SICK PAY - EMPLOYEES
            '603200':'632400000', // SICK PAY - OWNERS
            '604000':'633200000', // FICA TAX (INSURANCE CONTRIBUTION TAX - EMPLOYER)
            '604500':'633700000', // FEDERAL / STATE UNEMPLOYMENT (UNEMPLOYMENT INSURANCE - EMPLOYER)
            '605000':'634000000', // GROUP HEALTH INSURANCE
            '605100':'634100000', // GROUP HEALTH INSURANCE - PREMIUMS
            '605200':'634200000', // GROUP HEALTH INSURANCE - PREMIUMS OWNER or STOCKHOLDER
            '605800':'634800000', // EMPLOYEE HEALTH INSURANCE PREMIUM CONTRIBUTIONS
            '606000':'635000000', // WORKERS COMP. INSURANCE
            '607100':'636100000', // DISABILITY INCOME INSURANCE PREMIUMS
            '607200':'636200000', // DISABILITY INCOME INSURANCE PREMIUMS OWNER or STOCKHOLDER
            '607400':'636400000', // EMPLOYEE DISABILITY INSURANCE CO-PAYMENTS
            '607500':'636500000', // LIFE INSURANCE
            '607600':'636600000', // GROUP LIFE INSURANCE PREMIUMS
            '607700':'636700000', // GROUP LIFE INSURANCE PREMIUMS OWNER or STOCKHOLDER
            '607900':'636900000', // EMPLOYEE LIFE INSURANCE CO-PAYMENTS
            '608000':'637000000', // RETIREMENT (PENSION / PROFIT SHARING / 401(k)
            '608001':'222600000', // PAYROLL 401K CONTRIBUTIONS (401k - PENSION WITHHOLDING)
            '608002':'637000000', // COMPANY CONTRIB TO 401(K)
            '609000':'638000000', // OTHER BENEFIT EXPENSE
            '609001':'638100000', '609100':'638100000', // EMPLOYEE GIFTS
            '609200':'638200000', // STAFF PICNIC / CHRISTMAS PARTY (100% DEDUCTIBLE)
            '609201':'638300000', // STAFF MEALS AND SNACKS (100% DEDUCTIBLE)
            '609500':'639000000', // PAYROLL PROCESSING EXPENSE

            '611000':'611100000', // RENT EXPENSE
            '611100':'611110000', // BASIC RENT
            '611200':'611120000', // PERCENTAGE RENT
            '611300':'611130000', // COMMON AREA CHARGES
            '612000':'611200000', // UTILITIES
            '612100':'611210000', // UTILITIES - HEATING
            '612200':'611220000', // UTILITIES - ELECTRICITY
            '612300':'611230000', // UTILITIES - WATER AND SEWER
            '612400':'611240000', // UTILITIES - TRASH REMOVAL / RECYCLING
            '613000':'611300000', // OTHER OCCUPANCY COSTS
            '613100':'613500000', // CONDO MAINTENANCE (REPAIRS AND MAINTENANCE - RENTAL BUILDING)
            '613200':'611320000', // JANITORIAL AND OTHER CONTRACT SERVICES
            '613300':'613100000', // BUILDING MAINTENANCE (REPAIRS AND MAINTENANCE - BUILDING)

            '620000':'620000000', // MARKETING EXPENSES
            '621000':'621000000', '621100':'621000000', '621200':'621000000', '621300':'621000000', '621400':'621000000', '621500':'621000000', // ADVERTISING (consolidated)
            '622000':'653100000', '622100':'653100000', '622200':'653100000', // CATALOG / NEWSLETTER (reorganized with Postage Expense)
            '623000':'623000000', '623100':'623000000', '623200':'623000000', // SPECIAL EVENTS (consolidated)
            '624000':'624000000', '624100':'624000000', '624200':'624000000', '624300':'624000000', '624400':'624000000', // JOINT ADVERTISING (consolidated)
            '625000':'625000000', // OTHER ADVERTISING / PROMOTION
            '626000':'626000000', // PUBLISHER / VENDOR ADVERTISING ALLOWANCES
            '626401':'681110000', '626402':'681110000', '626403':'681110000', '626404':'681110000', '626405':'681110000', // TRAVEL AND TRADE SHOWS (consolidated)

            '626406':'627000000', // PROMOTIONAL MATERIALS
            '626407':'627100000', // PROMOTIONAL LITERATURE
            '626408':'627200000', // PROMOTIONAL DISPLAYS

            '630000':'614000000', // COMMUNICATIONS
            '631000':'614100000', // TELEPHONE COMPANY USE CHARGES
            '631100':'614110000', // TELEPHONE CO CHARGES - LOCAL
            '631200':'614120000', // TELEPHONE CO CHARGES - LONG DISTANCE
            '631300':'614130000', // TELEPHONE CO CHARGES - FAX
            '631400':'614140000', // TELEPHONE CO CHARGES - CELL PHONE AND PAGER
            '632000':'614200000', // NETWORK CHARGES

            '640000':'640000000', // PROFESSIONAL SERVICES
            '641000':'641000000', // LEGAL FEES
            '642000':'642000000', // ACCOUNTING FEES
            '643000':'643000000', // INVENTORY VERIFICATION
            '643001':'644000000', // MARKETING CONSULTING
            '649000':'649000000', // OTHER CONSULTING FEES

            '650000':'652000000', // SUPPLIES EXPENSE
            '651000':'652100000', // STATIONERY AND SUPPLIES - OFFICE USE
            '652000':'652200000', // WRAPPINGS AND BAGS
            '653000':'652300000', // PRINTED ITEMS - NOT OTHERWISE ALLOCATED
            '654000':'652400000', // JANITORIAL SUPPLIES
            '659000':'652900000', // OTHER SUPPLIES

            '661000':'616100000', // DATA PROCESSING SUPPLIES
            '662000':'616900000', // OTHER DATA PROCESSING EXPENSE
            '663000':'616300000', // OUTSIDE COMPUTER SERVICES
            '669000':'616400000', // WEB SITE AND SOFTWARE

            '670000':'670000000', // DEPRECIATION EXPENSE
            '671000':'671200000', // DEPRECIATION - BUILDING
            '672000':'671300000', // DEPRECIATION - LAND IMPROVEMENTS
            '674000':'671500000', // DEPRECIATION - LEASEHOLD IMPROVEMENTS
            '675000':'672100000', // DEPRECIATION - FURNITURE /FIXTURES / EQUIPMENT
            '675100':'672100000', // DEPRECIATION - FURNITURE /FIXTURES / EQUIPMENT
            '675200':'672200000', // DEPRECIATION - DATA PROCESSING EQUIPMENT
            '675300':'672300000', // DEPRECIATION - DATA PROCESSING SOFTWARE
            '675400':'672400000', // DEPRECIATION - VEHICLES

            '680000':'681000000', // TRAVEL AND ENTERTAINMENT
            '681000':'681100000', // BUSINESS TRAVEL
            '682000':'681200000', // BUSINESS MEALS AND ENTERTAINMENT (50% DEDUCTIBLE)
            '683000':'681300000', // FOOD - STAFF MEETINGS, ETC.  (100% DEDUCTIBLE - DE MINIMIS)

            '683001':'681110000', // TRADE SHOW TRAVEL EXPENSE
            '683002':'681210000', // TRADE SHOW MEALS AND ENTERTAINMENT
            '683003':'681110000', // SALES TRAVEL EXPENSE
            '683004':'681210000', // SALES MEALS AND ENTERTAINMENT
            '683005':'681110000', // SALES MANAGEMENT - EXPENSES (TRAVEL)
            '683006':'681210000', // SALES MANAGEMENT - MEALS AND ENTERTAINMENT

            '690000':'690000000', // INSURANCE

            '691000':'691000000', // BUSINESS INSURANCE
            '692000':'692000000', // REAL ESTATE INSURANCE
            '693000':'693000000', // VEHICLE INSURANCE
            '694000':'694000000', // OTHER INSURANCE

            '700000':'799000000', // OTHER EXPENSES
            '701000':'711000000', // CREDIT CARD SERVICE CHARGES
            '701100':'711100000', // CREDIT CARD SERVICE CHARGE MASTER CARD / VISA
            '701200':'711200000', // CREDIT CARD SERVICE CHARGE AMEX
            '701300':'711300000', // CREDIT CARD SERVICE CHARGE DISCOVER
            '701400':'711400000', // CREDIT CARD SERVICE CHARGE DEBIT / ATM CARD
            '702000':'712000000', // BANK SERVICE CHARGES
            '709000':'719000000', // OTHER SERVICE CHARGES

            '710000':'683000000', // DUES AND SUBSCRIPTIONS
            '711000':'683100000', // ASSOCIATION MEMBERSHIP FEES
            '712000':'683200000', // SUBSCRIPTION FEES
            '712100':'683210000', // SUBSCRIPTION FEES - PROFESSIONAL PUBLICATIONS
            '712200':'683220000', // SUBSCRIPTION FEES - BOUND REFERENCE TOOLS
            '712300':'683230000', // SUBSCRIPTION FEES - ELECTRONIC REFER TOOLS

            '720000':'651000000', // OFFICE EXPENSE
            '721000':'651000000', // OFFICE EXPENSE

            '730000':'653000000', // SHIPPING EXPENSE
            /* TODO: mapping this to COGS accounts but for many companies should be mapped to the expense account:
                '731000':'653100000', // POSTAGE EXPENSE */
            '731000':'519200000', // POSTAGE EXPENSE
            '739000':'653200000', // OTHER POSTAGE EXPENSE
            '731100':'519300000', // CUSTOMER PACKAGE CHARGES
            '731200':'441000000', // POSTAGE AND HANDLING FEES RECEIVED

            '741000':'741000000', // INVENTORY AND USE TAXES
            '742000':'742000000', // REAL ESTATE TAXES
            '743000':'743000000', // BUSINESS LICENSES AND FEES
            '744000':'744000000', // OTHER BUSINESS TAXES AND FEES

            '750000':'682000000', // EDUCATION
            '751000':'682100000', // EDUCATION - COURSE FEES
            '752000':'682200000', // EDUCATION - TRAVEL
            '753000':'682300000', // EDUCATION - MEALS AND ENTERTAINMENT ( 50% DEDUCTIBLE)
            '754000':'682400000', // EDUCATION - OTHER

            '761000':'612100000', // OFFICE EQUIPMENT RENT
            '762000':'612200000', // STORE EQUIPMENT RENT
            '763000':'612400000', // OTHER EQUIPMENT RENT

            '770000':'613000000', // REPAIRS AND MAINTENANCE
            '771000':'613100000', // REPAIRS AND MAINTENANCE - BUILDING
            '772000':'613200000', // REPAIRS AND MAINTENANCE - FURNITURE, FIXTURES, AND EQUIPMENT
            '773000':'613300000', // REPAIRS AND MAINTENANCE - DATA PROCESSING EQUIP
            '774000':'613400000', // REPAIRS AND MAINTENANCE - VEHICLE
            '779000':'613900000', // REPAIRS AND MAINTENANCE - OTHER

            '780000':'780000000', // OTHER OPERATING EXPENSES
            '781000':'781000000', // BAD DEBTS
            '782000':'781100000', // COLLECTION EXPENSE
            '783000':'783000000', // CASH OVER / SHORT

            '784000':'655000000', // CLASSIFIED ADS - HELP WANTED => Recruiting and Help Wanted
            '785000':'785000000', // CONTRIBUTIONS
            '786000':'613400000', // VEHICLE EXPENSE
            '786001':'613400000', '786002':'613400000',
            '787000':'787000000', // AMORTIZATION EXPENSE
            '788000':'798000000', // PENALTIES
            '789000':'794000000', // FRANCHISE FEE / ROYALTY
            '790000':'799000000', // MISCELLANEOUS EXPENSE
            '790001':'682500000', // PRODUCT RESEARCH
            '790002':'792000000', // KEYMAN INSURANCE (Company Owned Life Insurance)

            '800000':'800000000', // OTHER INCOME
            '802000':'832000000', // RENTAL INCOME
            '803000':'833000000', // SALES TAX COMMISSION
            '804000':'834000000', // SPECIAL FEES COLLECTED
            '805000':'835000000', // OTHER FEES COLLECTED
            '806000':'836000000', // MEMBERSHIP FEES COLLECTED

            '810000':'841000000', // INTEREST INCOME ON FINANCE CHARGES OR CUSTOMER ACCOUNTS
            '811000':'842000000', // OTHER INTEREST INCOME
            '812000':'812000000', // DIVIDEND INCOME
            '813000':'813000000', // CAPITAL GAINS INCOME
            '814000':'814100000', // GAIN ON SALE OF FIXED ASSETS
            '819000':'819000000', // OTHER INCOME

            '820000':'790000000', // OTHER EXPENSE
            '821000':'791000000', // INTEREST EXPENSE
            '822000':'791300000', // MORTGAGE INTEREST EXPENSE
            '823000':'793100000', // LOSS ON SALE OF FIXED ASSETS
            '824000':'795000000', // UNINSURED CASUALTY LOSS
            '829000':'799000000', // OTHER EXPENSES
            '850000':'850000000', // NET INCOME

            '901000':'731000000', // FEDERAL INCOME TAX
            '902000':'732000000', // STATE INCOME TAX
            '903000':'733000000', // LOCAL INCOME TAX
        ]
    ]
}
