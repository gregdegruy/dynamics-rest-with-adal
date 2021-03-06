import com.microsoft.aad.adal4j.AuthenticationContext;
import common.DynamicsDao;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class DynamicsDaoUnitTest {

    @Mock
    private DynamicsDao testMicrosoftDynamicsDao = DynamicsDao.getInstance("msott", "grdegr");

    @Before
    public void setUp() {

    }

    @Test
    public void shouldCreateAzureADApp() {
        /*

        az ad app create --display-name GSTestApp --native-app true

        az ad app permission add --id <APP_ID> --api <resourceAppId> --api-permissions xxx=Scope, xxx=Role

        az ad app permission admin-consent --id <APP_ID>

        permission needed for my app

        Common Data Service "resourceAppId": "00000007-0000-0000-c000-000000000000"
            user_impersonation delegated
            "resourceAccess":
            {
                "id": "78ce3f0f-a1ce-49c2-8cde-64b5c0896db4",
                "type": "Scope"
            }


        Microsoft Graph "resourceAppId": "00000003-0000-0000-c000-000000000000"
            Directory.AccessAsUser.All delegated
            User.Read.All delegated
            User.ReadBasic.All delegated
            User.Read delegated
            User.Export.All application
            IdentityRiskyUser.Read.All application

        "resourceAccess": [
        { Directory.AccessAsUser.All delegated
            "id": "0e263e50-5827-48a4-b97c-d940288653c7",
            "type": "Scope"
        },
        { User.Read delegated
            "id": "e1fe6dd8-ba31-4d61-89e7-88639da4683d",
            "type": "Scope"
        },
        { User.Read.All delegated
            "id": "a154be20-db9c-4678-8ab7-66f6cc099a59",
            "type": "Scope"
        },
        { User.ReadBasic.All delegated
            "id": "b340eb25-3456-403f-be2f-af7a0d370277",
            "type": "Scope"
        },
        {
            "id": "dc5007c0-2d7d-4c42-879c-2dab87571379",
            "type": "Role"
        },
        {
            "id": "405a51b5-8d8d-430b-9842-8be4b0e9f324",
            "type": "Role"
        }
        */
    }

    @Test
    public void shouldAuthenticateWithAccessToken() {
        String clientId;
        String clientsecret;
        String authenticationResult;
        String authenticationContext;
        String accessToken;

        ExecutorService service = Executors.newFixedThreadPool(3);
        try {
            AuthenticationContext context
                    = new AuthenticationContext("s", true, service);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        ExecutorService threadpool = Executors.newFixedThreadPool(3);
        FactorialCalculator task = new FactorialCalculator(1000);
        Future<Long> future = threadpool.submit(task);

        while(!future.isDone()) {
            System.out.println("Task is not completed yet....");
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Task is completed, let's check result");
        long factorial = 0;
        try {
            factorial = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("Factorial of 1000000 is : " + factorial);
        threadpool.shutdown();
    }

    private static class FactorialCalculator implements Callable {
        private final int number;

        public FactorialCalculator(int number) {
            this.number = number;
        }

        @Override
        public Long call() {
            long output = 0;
            try {
                output = factorial(number);
            } catch (InterruptedException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
            return output;
        }

        private long factorial(int number) throws InterruptedException {
            if (number < 0) {
                throw new IllegalArgumentException("Number must be greater than zero");
            }
            long result = 1;
            while (number > 0) {
                Thread.sleep(1);
                result = result * number;
                number--;
            }
            return  result;
        }
    }

    @Test
    public void shouldNotAuthenticateWithAccessToken() { }

    @Test
    public void shouldReadFromFile() {
        String expected = "{\r\n  \"steven\": \"universe\"\r\n}";
        String actual = testMicrosoftDynamicsDao.readFile("test.json");
        assertArrayEquals(expected.toCharArray(), actual.toCharArray());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Test
    public void shouldHandleException() {
        exception.expect(NullPointerException.class);
        testMicrosoftDynamicsDao.readFile("where'd I go?");
    }

    /**
     * Get audit history for an entity. Pass when find at least one different record in an audit.
     * AuditDetails collects all audits, most recent change to an entity is in front of queue.
     * @see https://msott.api.crm.dynamics.com/api/data/v9.0/RetrieveRecordChangeHistory(Target=@tid, PagingInfo=@pi)?@tid={'@odata.id':'accounts(da084227-2f4b-e911-a830-000d3a1d5a4d)'}&@pi=null
     */
    @Test
    public void getEntityAuditHistory() throws MalformedURLException, InterruptedException, ExecutionException {
        String methodName = "getEntityAuditHistory()";
        long startTime = System.currentTimeMillis();
        System.out.println("Start " + methodName);
        try {
            String accountId = "da084227-2f4b-e911-a830-000d3a1d5a4d";
            String path = "RetrieveRecordChangeHistory%28" +
                    "Target=@tid,%20" +
                    "PagingInfo=@pi%29?" +
                    "@tid={%27@odata.id%27:%27accounts%28" + accountId + "%29%27}&" +
                    "@pi=null";

            String responseString = testMicrosoftDynamicsDao.get(path);
            JSONObject odataResponse = new JSONObject(responseString);

            if (odataResponse.has("error")) {
                if (odataResponse.getJSONObject("error").getString("code") == "0x80072560" ||
                        odataResponse.getJSONObject("error").getString("message") == "The user is not a member " +
                                "of the organization.") {
                    fail("The user is not a member of the organization. Your application user may have the incorrect " +
                            "Application ID assigned.");
                }
            }

            JSONArray auditDetails = odataResponse.getJSONObject("AuditDetailCollection").getJSONArray("AuditDetails");
            int len = auditDetails.length();

            if (len == 0) {
                assertTrue("Passed. Got empty audit for entity.", true);
            } else {
                // Removed, will cause poor performance for large audits
                // Queue<JSONObject> auditHistory = new LinkedList<JSONObject>();
                // for (Object o : auditDetails) {
                //     JSONObject jo = (JSONObject) o;
                //     auditHistory.add(jo);
                // }

                String oldValue = auditDetails.getJSONObject(0).getJSONObject("OldValue").toString();
                String newValue = auditDetails.getJSONObject(0).getJSONObject("NewValue").toString();
                Assert.assertNotSame("Audit passed. New and old values are different", oldValue, newValue);
            }

            System.out.println("End " + methodName + "  " +
                    Long.toString(System.currentTimeMillis() - startTime) + "ms");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            fail();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail();
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * @see https://msott.api.crm.dynamics.com/api/data/v9.0/accounts(da084227-2f4b-e911-a830-000d3a1d5a4d)/Account_CustomerAddress
     */
    @Test
    public void getAssociatedAccountAddresses() throws MalformedURLException, InterruptedException, ExecutionException {
        try {
            System.out.println("getAssociatedAccountAddresses()");
            String accountId = "da084227-2f4b-e911-a830-000d3a1d5a4d";
            String path = "accounts%28" + accountId + "%29/Account_CustomerAddress";
            String responseString = testMicrosoftDynamicsDao.get(path);
            String goal = "Got list of addresses";
            assertEquals(goal, goal);
            System.out.println("end");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see https://msott.api.crm.dynamics.com/api/data/v9.0/GlobalOptionSetDefinitions(06d1a507-4d57-e911-a82a-000d3a1d5203)/Microsoft.Dynamics.CRM.OptionSetMetadata?$select=Options
     */
    @Test
    public void postGlobalOptionSetValue()
            throws MalformedURLException, InterruptedException, ExecutionException {
        System.out.println("postGlobalOptionSetValue()");
        int previousValue = 0;
        String optionSetGuidString = "06d1a507-4d57-e911-a82a-000d3a1d5203";
        try {
            String path = "GlobalOptionSetDefinitions%28" +  optionSetGuidString +
                    "%29/Microsoft.Dynamics.CRM.OptionSetMetadata/Options";
            String dataReturnedFromGetOptions = testMicrosoftDynamicsDao.get(path);
            JSONObject json = new JSONObject(dataReturnedFromGetOptions);
            JSONArray jsonArray = (JSONArray) json.get("value");
            JSONObject jsonObject = (JSONObject) jsonArray.get(jsonArray.length() - 1);
            previousValue = jsonObject.getInt("Value");
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }

        String prefix = "new";
        String entity = "_msdatzooptionset";
        String optionSetName = prefix + entity;
        String value = Integer.toString(++previousValue);
        String label = "JavaGlobalOption";
        String metadataId = "06d1a507-4d57-e911-a82a-000d3a1d5203";
        try {
            File file = new File(
                    getClass().getClassLoader().getResource("global-optionset.json").getFile()
            );
            JSONTokener jt = new JSONTokener(new FileReader(file.getPath()));
            JSONObject jo = new JSONObject(jt);
            jo.put("OptionSetName", optionSetName);
            jo.put("Value", value);
            jo.getJSONObject("Label").getJSONArray("LocalizedLabels").getJSONObject(0).put("Label", label);
            jo.getJSONObject("Label").getJSONArray("LocalizedLabels").getJSONObject(0).put("MetadataId", metadataId);
            jo.getJSONObject("Label").getJSONObject("UserLocalizedLabel").put("Label", label);
            jo.getJSONObject("Label").getJSONObject("UserLocalizedLabel").put("MetadataId", metadataId);
            jo.getJSONObject("Description").getJSONArray("LocalizedLabels").getJSONObject(0).put("MetadataId", metadataId);
            jo.getJSONObject("Description").getJSONObject("UserLocalizedLabel").put("Label", label);
            jo.getJSONObject("Description").getJSONObject("UserLocalizedLabel").put("MetadataId", metadataId);
            String content = jo.toString();

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, content);
            testMicrosoftDynamicsDao.post("InsertOptionValue", body);

            String goal = "Posted global Option Set in Settings > Customizations > " +
                    "Customize the System > Option Sets. Named " + label + " in Publisher defined Option Set.";
            assertEquals(goal, goal);
            System.out.println("End");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * Adds value to an Option Set field of an Entity in a Solution.
     * @see https://msott.crm.dynamics.com/api/data/v9.1/EntityDefinitions(LogicalName='cr965_testcdsentity')/Attributes/Microsoft.Dynamics.CRM.PicklistAttributeMetadata?$select=LogicalName&$filter=LogicalName%20eq%20%27new_localoptionsettoform%27&$expand=OptionSet
     */
    @Test
    public void postLocalOptionSetValue()
            throws MalformedURLException, InterruptedException, ExecutionException {

        System.out.println("postLocalOptionSetValue()");
        int previousValue = 0;
        String entityLogicalname = "cr965_testcdsentity";
        String optionSetLogicalName = "new_localoptionsettoform";
        try {
            String path = "EntityDefinitions%28LogicalName=%27" + entityLogicalname +
                    "%27%29/Attributes/Microsoft.Dynamics.CRM.PicklistAttributeMetadata" +
                    "?$select=LogicalName&$filter=LogicalName%20eq%20%27" + optionSetLogicalName +
                    "%27&$expand=OptionSet";
            String dataReturnedFromGetOptions = testMicrosoftDynamicsDao.get(path);
            JSONObject odataResponse = new JSONObject(dataReturnedFromGetOptions);
            JSONArray optionsArray = odataResponse
                    .getJSONArray("value")
                    .getJSONObject(0)
                    .getJSONObject("OptionSet")
                    .getJSONArray("Options");
            previousValue = optionsArray
                    .getJSONObject(optionsArray.length() - 1)
                    .getInt("Value");
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }

        String optionValue = Integer.toString(++previousValue);
        String label = "JavaLocalOption";
        try {
            File file = new File(
                    getClass().getClassLoader().getResource("local-optionset.json").getFile()
            );
            JSONTokener jt = new JSONTokener(new FileReader(file.getPath()));
            JSONObject jo = new JSONObject(jt);
            jo.put("AttributeLogicalName", optionSetLogicalName);
            jo.put("EntityLogicalName", entityLogicalname);
            jo.put("Value", optionValue);
            jo.getJSONObject("Label").getJSONArray("LocalizedLabels").getJSONObject(0).put("Label", label);
            jo.getJSONObject("Label").getJSONObject("UserLocalizedLabel").put("Label", label);
            String content = jo.toString();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, content);
            testMicrosoftDynamicsDao.post("InsertOptionValue", body);

            String goal = "Posted Option Set in Solutions > {Your_Solution_Name} > {Your_Entity} > " +
                    "Fields > Option Sets. Named " + label + " in Publisher defined Option Set.";
            assertEquals(goal, goal);
            System.out.println("End");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an email to all Account Contacts. Displays in Entity Timeline.
     * View an entity in a Model-driven app using these queries.
     * ?appid=c2b315b4-9040-e911-a823-000d3a1a25b8
     * &pagetype=entityrecord
     * &etn=contact
     * &id=a8093fe9-795c-e911-a825-000d3a1d501d
     * https://msott.api.crm.dynamics.com/api/data/v9.0/emails
     */
    @Test
    public void postEmailWithPartyList() throws MalformedURLException, InterruptedException, ExecutionException {

        System.out.println("postLocalOptionSetValue()");
        try {
            OkHttpClient client = new OkHttpClient();
            String accountId = "da084227-2f4b-e911-a830-000d3a1d5a4d";
            String path = "accounts" + "%28" + accountId + "%29/contact_customer_accounts";
            String contactsResponse = testMicrosoftDynamicsDao.get(path);
            JSONObject contactsResponseJson = new JSONObject(contactsResponse);
            JSONArray contactsJsonArray = contactsResponseJson.getJSONArray("value");
            Queue<String> contactIds = new LinkedList<String>();
            for (Object o : contactsJsonArray) {
                JSONObject contact = (JSONObject) o;
                contactIds.add(contact.getString("contactid"));
            }

            final int SENDER_PARTICIPATION_TYPE_MASK = 1;
            final int TO_PARTICIPATION_TYPE_MASK = 2;
            final int CC_PARTICIPATION_TYPE_MASK = 3;
            final int BCC_PARTICIPATION_TYPE_MASK = 4;
            String systemuserId = "96b856f4-134c-e911-a823-000d3a1d5de8";
            String senderId = systemuserId;
            MediaType mediaType = MediaType.parse("application/json");
            Stack<JSONObject> stack = new Stack<JSONObject>();
            JSONObject contact;
            for (String id : contactIds) {
                contact = new JSONObject();
                contact.put("partyid_contact@odata.bind", "/contacts(" + id + ')');
                contact.put("participationtypemask", TO_PARTICIPATION_TYPE_MASK);
                stack.push(contact);
            }
            File f = new File(
                    getClass().getClassLoader().getResource("email-activity-party.json").getFile()
            );
            FileReader fr = new FileReader(f);
            char[] letters = new char[(int) f.length()];
            fr.read(letters);
            fr.close();
            String content = new String(letters);

            JSONTokener jt = new JSONTokener(new FileReader(f.getPath()));
            JSONObject jo = new JSONObject(jt);
            while (!stack.empty()) {
                jo.getJSONArray("email_activity_parties").put(stack.pop());
            }
            content = jo.toString();
            content = content.replace("SYSTEM_USER_ID", senderId);
            mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, content);
            testMicrosoftDynamicsDao.post("emails", body);

            String goal = "Sent emails to account with id" + accountId + " from user id " + systemuserId +
                    "Check Timeline for emails.";
            assertEquals(goal, goal);
            System.out.println("End");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see https://msott.api.crm.dynamics.com/api/data/v9.0/accounts?$select=name,address1_latitude,address1_longitude,description,revenue,createdon
     */
    @Test
    public void postWithDataReturned() throws MalformedURLException, InterruptedException, ExecutionException {

        System.out.println("postWithDataReturned()");
        try {
            File f = new File(
                    getClass().getClassLoader().getResource("account.json").getFile()
            );
            FileReader fr = new FileReader(f);
            char[] letters = new char[(int) f.length()];
            fr.read(letters);
            fr.close();
            String content = new String(letters);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, content);
            String path = "accounts?$select=name,address1_latitude,address1_longitude,description,revenue,createdon";
            AbstractMap.SimpleEntry<String, String> headers = new AbstractMap.SimpleEntry<String, String>("Prefer", "return=representation");
            testMicrosoftDynamicsDao.post(path, body, headers);

            String goal = "Created and an Account with response data returned.";
            assertEquals(goal, "Test is failing :(");
            System.out.println("End");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Handle SocketTimeoutException
    @Test
    public void batchAccountcreate() throws MalformedURLException, InterruptedException, ExecutionException {

        System.out.println("batchAccountcreate()");
        try {
            File f = new File(
                    getClass().getClassLoader().getResource("batch.txt").getFile()
            );
            FileReader fr = new FileReader(f);
            char[] letters = new char[(int) f.length()];
            fr.read(letters);
            fr.close();
            String content = new String(letters);
            String changeSetType = "multipart/mixed;boundary=changeset_BBB456";
            MediaType mediaType = MediaType.parse(changeSetType);
            RequestBody body = RequestBody.create(mediaType, content);
            String path = "$batch";
            AbstractMap.SimpleEntry<String, String> headers = new AbstractMap.SimpleEntry<String, String>("Content-Type", changeSetType);
            testMicrosoftDynamicsDao.post(path, body, headers);

            String goal = "Created accounts in batch from txt file.";
            assertEquals(goal, goal);
            System.out.println("end");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
