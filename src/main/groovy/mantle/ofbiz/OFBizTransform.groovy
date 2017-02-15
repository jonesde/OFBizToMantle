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
import org.moqui.context.ExecutionContext
import org.moqui.etl.SimpleEtl
import org.moqui.etl.SimpleEtl.EntryTransform
import org.moqui.etl.SimpleEtl.SimpleEntry
import org.moqui.etl.SimpleEtl.Transformer
import static mantle.ofbiz.OFBizFieldMap.map

@CompileStatic
class OFBizTransform {
    static List<String> loadOrder = ['Party', 'Person', 'PartyGroup', 'PartyRole', 'PartyClassification', 'PartyRelationship', 'UserLogin'
            /*'ContactMech', 'PartyContactMechPurpose', 'PostalAddress', 'TelecomNumber'*/]
    static List<List<String>> loadOrderParallel = [
            ['Party', 'ContactMech'],
            ['Person', 'PartyGroup', 'PartyRole', 'UserLogin', 'PartyContactMechPurpose', 'PostalAddress', 'TelecomNumber'],
            ['PartyClassification', 'PartyRelationship']]

    static SimpleEtl.TransformConfiguration conf = new SimpleEtl.TransformConfiguration()
    static {
        conf.addTransformer("Party", new Transformer() { void transform(EntryTransform et) { Map<String, Object> val = et.entry.etlValues
            et.addEntry(new SimpleEntry("mantle.party.Party", [partyId:val.partyId, partyTypeEnumId:map('partyTypeId', (String) val.partyTypeId),
                externalId:val.externalId, disabled:("PARTY_DISABLED".equals(val.statusId) ? 'N' : 'Y'), lastUpdatedStamp:((String) val.lastUpdatedTxStamp).take(23)]))
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
                ExecutionContext ec = Moqui.getExecutionContext()
                if (ec.entity.find("mantle.party.RoleType").condition("roleTypeId", roleTypeId).useCache(true).one() == null) {
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

        /*

         */

        // =========== Custom Transformers ===========
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
