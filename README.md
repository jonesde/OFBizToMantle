# OFBizToMantle

Moqui component to transform and load Apache OFBiz Entity XML data to Mantle UDM

Transforms and loads:

- Party, Person, PartyGroup, PartyNote/NoteData, PartyRole, PartyRelationship, PartyClassification, UserLogin
- ContactMech, PostalAddress, TelecomNumber, PartyContactMechPurpose
- PaymentMethod, CreditCard
- Product, ProductPrice
- InventoryItem, InventoryItemDetail, Lot, PhysicalInventory, OrderItemShipGrpInvRes, ItemIssuance, ShipmentReceipt 
- OrderHeader, OrderItem, OrderAdjustment, OrderItemShipGroup, OrderPaymentPreference, OrderHeaderNote, OrderContactMech, OrderRole, OrderItemBilling, OrderAdjustmentBilling, OrderShipment
- Shipment, ShipmentItem, ShipmentBoxType, ShipmentPackage, ShipmentPackageContent, ShipmentRouteSegment, ShipmentPackageRouteSeg
- Invoice, InvoiceItem, InvoiceContactMech, InvoiceRole
- Payment, PaymentApplication, PaymentGatewayResponse
- FinAccount, FinAccountTrans
- AcctgTrans, AcctgTransEntry, GlJournal

This migration does not load seed data in general (types, statuses, etc) and instead maps to Moqui/Mantle seed data.

GlAccount records are not loaded and instead there is a mapping from the default OFBiz chart of accounts to the default Mantle accounts.

Sets of entities are loaded in parallel with the sets determined on foreign key dependencies.

While this migration may work as-is it is very common to customize and extend OFBiz or to use data structures is ways other than how
they are intended so customization of this migration is likely to be needed.

Transformations are written in Groovy, current all in the OFBizTransform.groovy file. The type mappings are also in Groovy as simple
Map objects in the OFBizFieldMap.groovy file.

This is made to be run against a directory containing an OFBiz export with one file per entity (file name matching the entity name plus .xml).

To run this use the 'mantle.ofbiz.OFBizToMantle.import#OFBizData' service.
