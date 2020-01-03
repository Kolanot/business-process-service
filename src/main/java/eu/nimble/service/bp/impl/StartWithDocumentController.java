package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.nimble.service.bp.bom.BPMessageGenerator;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.FederatedCollaborationGroupMetadata;
import eu.nimble.service.bp.swagger.model.GroupIdTuple;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.UBLUtility;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.bp.ClassProcessTypeMap;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.serialization.IDocumentDeserializer;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

@Controller
public class StartWithDocumentController {
    private final Logger logger = LoggerFactory.getLogger(StartWithDocumentController.class);

    @Autowired
    private StartController startController;
    @Autowired
    private ContinueController continueController;
    @Autowired
    private IValidationUtil validationUtil;

    // TODO: we need to get a token for business-process service
    public static final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmMzEzOGMzOS1mMWM4LTRmMDYtOGJkZC0zMzBiM2I4ZmE2NTYiLCJleHAiOjE1MzAwODg3MzQsIm5iZiI6MCwiaWF0IjoxNTMwMDAyMzM0LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6ImIyMmQyZDE5LTNhY2ItNDUyMC1iNWFlLTdkOGU2MGQ3ODQ4YyIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.Un1K0t37Ln3VN51i-Is_";

    @ApiOperation(value = "",notes = "It starts a new process for request documents or completes the existing one for response documents." +
            "If the request document has a reference (document.additionalDocumentReference) with a 'previousDocument' type (document.additionalDocumentReference.documentType)," +
            "the new process is placed into the same ProcessInstanceGroup with this referenced process.If the reference type is 'previousOrder', then a new ProcessInstanceGroup" +
            "will be created for the process in the same CollaborationGroup with the referenced process.Otherwise, a new CollaborationGroup will be created for the process.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Processes the document successfully", response = ProcessInstance.class),
            @ApiResponse(code = 400, message = "Each response document should have a valid reference to the request document"),
            @ApiResponse(code = 404, message = "Seller party does not exist")
    })
    @RequestMapping(value = "/process-document",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity startProcessWithDocument(@ApiParam(value = "Serialized form of the document", required = true) @RequestBody String documentAsString,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = false)
                                          @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        logger.info("Getting request to start process with document");

        /**
         * If the bearer token is provided, it's used to set the creator user for the process.
         * Otherwise, the first user of party is set as the creator user for the process.
         * */
        String creatorUserId = null;
        // check the bearer token if it's provided
        if(bearerToken != null){
            // check token and get creator user id
            try {
                PersonType creatorUser = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
                if(creatorUser == null){
                    String msg = String.format("No user exists for the given token : %s", bearerToken);
                    return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(msg, null, HttpStatus.UNAUTHORIZED, LogLevel.INFO);
                }
                // set creator user id
                creatorUserId = creatorUser.getID();
            } catch (IOException e) {
                logger.error("Failed to get person",e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get person");
            }
        }

        // if no bearer token is provided, use the default one.
        if(bearerToken == null){
            bearerToken = token;
        }

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        /**
         * Deserialize the given document using the custom deserializer {@link IDocumentDeserializer}
         * */
        // get the ObjectMapper
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

        // add deserializer to be able deserialize IDocument instances properly
        SimpleModule module = new SimpleModule();
        module.addDeserializer(IDocument.class, new IDocumentDeserializer());
        mapper.registerModule(module);

        IDocument document;
        try {
            document = mapper.readValue(documentAsString,IDocument.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize document",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to deserialize document");
        }

        /**
         * Check whether the seller party exists or not
         * */
        String sellerPartyId = document.getSellerPartyId();
        try {
            PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,sellerPartyId);

            if(sellerParty == null){
                String msg = String.format("There does not exist a party for : %s", sellerPartyId);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
        } catch (IOException e) {
            String msg = String.format("Failed to retrieve party information for : %s", sellerPartyId);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        /**
         * Start or complete the business process instance
         * if the given document is a request document, use {@link StartController#startProcessInstance(String, ProcessInstanceInputMessage, String, String, String)} to start a process instance.
         * else, use {@link ContinueController#continueProcessInstance(ProcessInstanceInputMessage, String, String, String)} to complete the process instance.
         * */
        // check whether it is a request or response document
        boolean isInitialDocument = BusinessProcessUtility.isInitialDocument(document.getClass());

        // get corresponding process type
        String processId = ClassProcessTypeMap.getProcessType(document.getClass());
        // retrieve initiator and responder federation id from the given document
        String initiatorFederationId = document.getBuyerParty().getFederationInstanceID();
        String responderFederationId = document.getSellerParty().getFederationInstanceID();
        // for Fulfilment, it's vice versa
        if(processId.contentEquals(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_FULFILMENT)){
            initiatorFederationId = document.getSellerParty().getFederationInstanceID();
            responderFederationId = document.getBuyerParty().getFederationInstanceID();
        }

        ProcessInstance processInstance;
        if(isInitialDocument){
            // create ProcessInstanceInputMessage
            ProcessInstanceInputMessage processInstanceInputMessage;
            try {
                processInstanceInputMessage = BPMessageGenerator.createProcessInstanceInputMessage(document,document.getItemTypes(),creatorUserId,"",bearerToken);
            } catch (Exception e) {
                String msg = "Failed to create process instance input message for the document";
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

            // to start the process, we need to know the details of preceding process instance so that we can place the process instance to correct group
            ProcessDocumentMetadataDAO processDocumentMetadataDAO = getProcessDocumentMetadataDAO(document, false);
            String processInstanceId = processDocumentMetadataDAO != null ? processDocumentMetadataDAO.getProcessInstanceID(): null ;
            String precedingOrderId = getPrecedingOrderId(document);

            String cgid = null;
            String gid = null;
            String precedingGid = null;
            if(processInstanceId != null){
                // get the collaboration group containing the process instance for the initiator party
                CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(processInstanceInputMessage.getVariables().getInitiatorID(),initiatorFederationId,processInstanceId);
                cgid = collaborationGroup.getHjid().toString();
                // get the identifier of process instance group containing the process instance
                for(ProcessInstanceGroupDAO processInstanceGroupDAO:collaborationGroup.getAssociatedProcessInstanceGroups()){
                    if(processInstanceGroupDAO.getProcessInstanceIDs().contains(processInstanceId)){
                        gid = processInstanceGroupDAO.getID();
                        break;
                    }
                }
            }
            else if(precedingOrderId != null){
                try {
                    // get group id tuple for preceding order
                    Response response = SpringBridge.getInstance().getDelegateClient().getGroupIdTuple(bearerToken,initiatorFederationId,precedingOrderId,processInstanceInputMessage.getVariables().getInitiatorID(),initiatorFederationId);
                    String responseBody = HttpResponseUtil.extractBodyFromFeignClientResponse(response);

                    ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                    GroupIdTuple groupIdTuple = objectMapper.readValue(responseBody,GroupIdTuple.class);

                    // get the collaboration group containing the process instance for the initiator party
                    cgid = groupIdTuple.getCollaborationGroupId();
                    precedingGid = groupIdTuple.getProcessInstanceGroupId();
                }
                catch (Exception e){
                    String msg = String.format("Failed to get group id tuple for document  : %s", precedingOrderId);
                    logger.error(msg,e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
            processInstance = startController.startProcessInstance(bearerToken,initiatorFederationId,responderFederationId,processInstanceInputMessage,gid,precedingGid,precedingOrderId,cgid).getBody();

            if(processInstance == null){
                String msg = "Failed to start process for the given document";
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

        }
        else{
            // to complete the process, we need to know process instance id
            ProcessDocumentMetadataDAO processDocumentMetadataDAO = getProcessDocumentMetadataDAO(document, true);
            if(processDocumentMetadataDAO == null){
                String msg = "Each response document should have a valid reference to the request document";
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }
            String processInstanceId = processDocumentMetadataDAO.getProcessInstanceID();

            // since some response documents do not have items, we need to use the items of request document
            List<ItemType> items = document.getItemTypes() != null ? document.getItemTypes(): getItemTypes(processDocumentMetadataDAO.getDocumentID(),processDocumentMetadataDAO.getType());

            // create ProcessInstanceInputMessage
            ProcessInstanceInputMessage processInstanceInputMessage;
            try {
                processInstanceInputMessage = BPMessageGenerator.createProcessInstanceInputMessage(document,items,creatorUserId,processInstanceId,bearerToken);
            } catch (Exception e) {
                String msg = "Failed to create process instance input message for the document";
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

            // get the collaboration group containing the process instance for the responder party
            CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(processInstanceInputMessage.getVariables().getResponderID(),responderFederationId,processInstanceId);

            // get the identifier of process instance group containing the process instance
            String gid = null;
            for(ProcessInstanceGroupDAO processInstanceGroupDAO:collaborationGroup.getAssociatedProcessInstanceGroups()){
                if(processInstanceGroupDAO.getProcessInstanceIDs().contains(processInstanceId)){
                    gid = processInstanceGroupDAO.getID();
                    break;
                }
            }
            processInstance = continueController.continueProcessInstance(processInstanceInputMessage,gid,collaborationGroup.getHjid().toString(),bearerToken,initiatorFederationId,responderFederationId).getBody();
            if(processInstance == null){
                String msg = "Failed to complete process for the given document";
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

            /**
             * Send response document to the initiator party
             * */
            // get the initiator party id
            PartyType initiatorParty = document.getBuyerParty();
            // for Fulfilment, it's vice versa
            if(processId.contentEquals(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_FULFILMENT)){
                initiatorParty = document.getSellerParty();
            }

            // get the initiator party
            try {
                CodeType communicationChannel = UBLUtility.getPartyCommunicationChannel(initiatorParty);
                // send document to initiator party iff it's a Quotation or Order response
                if(communicationChannel != null && (document instanceof QuotationType || document instanceof OrderResponseSimpleType)){
                    String msg = createRequestBody(document,communicationChannel.getListID(),communicationChannel.getURI());
                    logger.info("Sending document {} to {}",msg, communicationChannel.getValue());
                    HttpResponse<String> response = Unirest.post(communicationChannel.getValue())
                            .header("Content-Type", "application/json")
                            .header("accept", "*/*")
                            .body(msg)
                            .asString();

                    if(response.getStatus() != 200 && response.getStatus() != 204){
                        logger.error("Failed send the document to the initiator party {}, endpoint: {} : {}",initiatorParty.getPartyIdentification().get(0).getID(), communicationChannel.getValue(), response.getBody());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    }
                }
            } catch (Exception e) {
                String msg = String.format("Failed to send the document to the initiator party : %s", initiatorParty.getPartyIdentification().get(0).getID());
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        logger.info("Completed the request to start process with document");
        return ResponseEntity.ok(processInstance);
    }

    /**
     * If the given document is a response document, this method simply returns {@link ProcessDocumentMetadataDAO} representing the corresponding request document.
     * Otherwise, if it has a reference called {@literal previousDocument}, {@link ProcessDocumentMetadataDAO} representing the previous document is returned.
     */
    private ProcessDocumentMetadataDAO getProcessDocumentMetadataDAO(IDocument document, Boolean isResponseDocument){
        String documentId = null;
        // if it is a response document, then we need to get the identifier of request document
        if(isResponseDocument){
            // get the request document id
            documentId = document.getRequestDocumentId();
        }
        // otherwise, we'll find the identifier of previous document
        else{
            for (DocumentReferenceType documentReferenceType : document.getAdditionalDocuments()) {
                if(documentReferenceType.getDocumentType() != null && documentReferenceType.getDocumentType().contentEquals("previousDocument")){
                    documentId = documentReferenceType.getID();
                }
            }
        }

        // get the process instance id
        return documentId != null ? ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId) : null;
    }

    /**
     * If the given document has a reference called {@literal previousOrder}, this method returns the identifier of process instance associated with that order.
     * */
    private String getPrecedingOrderId(IDocument document){
        String documentId = null;
        for (DocumentReferenceType documentReferenceType : document.getAdditionalDocuments()) {
            if(documentReferenceType.getDocumentType() != null && documentReferenceType.getDocumentType().contentEquals("previousOrder")){
                documentId = documentReferenceType.getID();
            }
        }
        return documentId;
    }

    /**
     * It retrieves the specified document and returns its {@link ItemType}
     * */
    private List<ItemType> getItemTypes(String documentId, DocumentType documentType){
        IDocument iDocument =  DocumentPersistenceUtility.getUBLDocument(documentId, documentType);
        return iDocument.getItemTypes();
    }

    /**
     * Prepare the json which has the following format:
     * {
     *   "messageName": "<message_name_here>",
     *   "processInstanceId": "<process_instance_id_here>",
     *   "processVariables": {
     *          "quotationData/orderData": {
     *              "id": "<id_of_document>"
     *              "type": "String",
     *              "value": {
     *                  "status ": "<status_of_document>",
     *                  "netPrice": "<net_price_of_document>"
     *              }
     *          }
     *   }
     * }
     * */
    private String createRequestBody(IDocument document, String messageName, String processInstanceId){
        // whether the document is quotation or not
        boolean isQuotation = document instanceof QuotationType;
        // info taken from the document
        String documentId = null;
        String price = null;
        String status = null;
        // Document is a Quotation
        if(isQuotation){
            QuotationType quotation = (QuotationType) document;
            price = new DecimalFormat(".00").format(quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getValue().multiply(quotation.getQuotationLine().get(0).getLineItem().getQuantity().getValue()))
                    + " " + quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID();
            status = quotation.getDocumentStatusCode().getName();
            documentId = quotation.getID();
        }
        // Document is an Order Response
        else{
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderResponse.getOrderReference().getDocumentReference().getID(),DocumentType.ORDER);

            price = new DecimalFormat(".00").format(order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue().multiply(order.getOrderLine().get(0).getLineItem().getQuantity().getValue()))
                    + " " + order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID();
            status = orderResponse.isAcceptedIndicator() ? "Accepted": "Rejected";
            documentId = orderResponse.getID();
        }

        JSONObject documentDetails = new JSONObject();
        documentDetails.put("netPrice",price);
        documentDetails.put("status ",status);

        JSONObject documentData = new JSONObject();
        documentData.put("id",documentId);
        documentData.put("type","String");
        documentData.put("value",documentDetails);

        JSONObject processVariables = new JSONObject();
        processVariables.put(isQuotation ? "quotationData" :"orderData",documentData);

        JSONObject json = new JSONObject();
        json.put("processVariables",processVariables);
        json.put("messageName",messageName);
        json.put("processInstanceId",processInstanceId);
        return json.toString();
    }
}