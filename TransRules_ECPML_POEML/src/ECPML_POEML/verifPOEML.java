package ECPML_POEML;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;


import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class verifPOEML {
	//public static void main(String[] args) throws Exception {
		//String path= "C:\\Users\\vappyq\\Documents\\test.xml";
		//verifPOEML.validateXML(path);
	//}

    public static void validateXML(File xmlFile) throws Exception {
        //String xmlFile = new File(xmlFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Validate that each 'performer' tag has a 'type' attribute of 'performer' or 'assistant'
        //NodeList performers = doc.getElementsByTagName("performer");
        //checkPerformer(doc);

        // Check if 'task' elements have a 'name' attribute
        NodeList tasks = doc.getElementsByTagName("Task");
        checkTasks(tasks);
        
        // Gather all IDs or names that can be referenced
        Set<String> referencableIds = gatherReferencableIds(doc);

        // Check references
        checkReferences(doc, referencableIds);

        // Check TaskParameter tags inside TaskParameters
        checkTaskParameter(doc);

        // Check TaskPerformer tags inside Tasks
        checkTaskPerformers(doc);

        // Product validations
        checkProducts(doc);

        // Role validations
        checkRoles(doc);

        // Check direction values in TaskParameter
        checkDirections(doc);
        
        checkKind (doc);
        
        //checkPerformer(doc);
        
        checkTaskPrecedence(doc);
        
        System.out.println("XML file" + xmlFile+" is correct");
        
    }

   /* private static void checkPerformer(Document doc) throws Exception {
        // Get all Task elements
        NodeList tasks = doc.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            // For each Task, check its TaskPerformers
            NodeList performers = task.getElementsByTagName("TaskPerformer");
            for (int j = 0; j < performers.getLength(); j++) {
                Element performer = (Element) performers.item(j);
                String type = performer.getAttribute("Type");
                if (!type.equals("Performer") && !type.equals("Assistant")) {
                    throw new Exception("TaskPerformer tag does not have the correct 'type' attribute at Task index: " + i);
                }
                // Check for exactly one Role tag
                NodeList roles = performer.getElementsByTagName("Role");
                if (roles.getLength() != 1) {
                    throw new Exception("TaskPerformer tag in Task index " + i + " must contain exactly one 'Role' tag, found: " + roles.getLength());
                }
                // Check for exactly one LinkedTask tag
                NodeList linkedTasks = performer.getElementsByTagName("LinkedTask");
                if (linkedTasks.getLength() != 1) {
                    throw new Exception("TaskPerformer tag in Task index " + i + " must contain exactly one 'LinkedTask' tag, found: " + linkedTasks.getLength());
                }
            }
        }
    }*/


    private static void checkTasks(NodeList tasks) throws Exception {
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            if (!task.hasAttribute("name")) {
                throw new Exception("Task element number:"+i+" missing 'name' attribute.");
            }
        }
    }

    private static void checkTaskParameter(Document doc) throws Exception {
        NodeList taskParameters = doc.getElementsByTagName("TaskParameter");
        
        for (int i = 0; i < taskParameters.getLength(); i++) {
            Element taskParameter = (Element) taskParameters.item(i);

            // Check for exactly one 'LinkedTask' tag
            NodeList linkedTasks = taskParameter.getElementsByTagName("LinkedTask");
            if (linkedTasks.getLength() != 1) {
                throw new Exception("There must be exactly one 'LinkedTask' tag in TaskParameter at index: " + i + ", found: " + linkedTasks.getLength());
            }

            // Check for exactly one 'direction' tag
            NodeList directions = taskParameter.getElementsByTagName("direction");
            if (directions.getLength() != 1) {
                throw new Exception("There must be exactly one 'direction' tag in TaskParameter at index: " + i + ", found: " + directions.getLength());
            }
            
            NodeList products = taskParameter.getElementsByTagName("product");
            if (products.getLength() != 1) {
                throw new Exception("There must be exactly one 'product' tag in TaskParameter at index: " + i + ", found: " + products.getLength());
            }
        }
    }


    private static void checkTaskPerformers(Document doc) throws Exception {
        NodeList taskPerformers = doc.getElementsByTagName("TaskPerformer");
        
        for (int i = 0; i < taskPerformers.getLength(); i++) {
            Element taskPerformer = (Element) taskPerformers.item(i);
            NodeList roles = taskPerformer.getElementsByTagName("Role");
            NodeList linkedTasks = taskPerformer.getElementsByTagName("LinkedTask");

            System.out.println("TaskPerformer index " + (i+1) + " Role count: " + roles.getLength());
            System.out.println("TaskPerformer index " + (i+1) + " LinkedTask count: " + linkedTasks.getLength());

            if (roles.getLength() != 1) {
                throw new Exception("There must be exactly one 'Role' tag in TaskPerformer at index: " + (i + 1) + ", found: " + roles.getLength());
            }
            if (linkedTasks.getLength() != 1) {
                throw new Exception("There must be exactly one 'LinkedTask' tag in TaskPerformer at index: " + (i + 1) + ", found: " + linkedTasks.getLength());
            }
        }
    }

    private static boolean checkForNonEmptyProduct(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList products = ((Element) nodeList.item(i)).getElementsByTagName("product");
            for (int j = 0; j < products.getLength(); j++) {
                Element product = (Element) products.item(j);
                if (product.hasAttributes()) {
                    return true; // Found a non-empty product by attribute
                }
            }
        }
        return false;
    }

    private static void checkProducts(Document doc) throws Exception {
        NodeList products = doc.getElementsByTagName("Product");
        for (int i = 0; i < products.getLength(); i++) {
            Element product = (Element) products.item(i);
            if (!product.hasAttribute("name")) {
                throw new Exception("Product tag missing 'name' attribute.");
            }
            boolean isComposite = Boolean.parseBoolean(product.getAttribute("isComposite"));
            
            // Check for non-empty product tags by attributes, not just any child node
            NodeList aggregations = product.getElementsByTagName("Aggregation");
            NodeList impactedProducts = product.getElementsByTagName("ImpactedProduct");

            boolean hasNonEmptyProduct = checkForNonEmptyProduct(aggregations) || checkForNonEmptyProduct(impactedProducts);

            if ((isComposite && !hasNonEmptyProduct)) {
                throw new Exception("Product composition doesn't exist even though 'isComposite' is true attribute");
            }else if((!isComposite && hasNonEmptyProduct)){
                throw new Exception("Product composition exists even though 'isComposite' is false attribute for product named '" + product.getAttribute("name") + "'.");
            }
        }
    }

   



    private static void checkRoles(Document doc) throws Exception {
        // Find the 'roles' parent element first
        NodeList rolesList = doc.getElementsByTagName("Roles");
        if (rolesList.getLength() == 0) {
            throw new Exception("No 'roles' element found in the document.");
        }
        for (int i = 0; i < rolesList.getLength(); i++) {
        Node roleNode = rolesList.item(i);
        if (roleNode.getNodeType() == Node.ELEMENT_NODE) {
            Element roleElement = (Element) roleNode;
            NodeList roleElements = roleElement.getElementsByTagName("Role");
            for (int j = 0; j < roleElements.getLength(); j++) {
                Element role = (Element) roleElements.item(j);
                if (!role.hasAttribute("name")) {
                    throw new Exception("Role tag missing 'name' attribute within 'roles'.");
                }
            }
        }
        }
     }
    
    private static void checkTaskPrecedence(Document doc) throws Exception {
        NodeList taskPrecedences = doc.getElementsByTagName("TaskPrecedence");
        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element taskPrecedence = (Element) taskPrecedences.item(i);

            // Check for exactly one 'LinkedTask' tag
            NodeList successor = taskPrecedence.getElementsByTagName("successor");
            if (successor.getLength() != 1) {
                throw new Exception("There must be exactly one 'successor' tag in TaskPrecedence at index: " + i + ", found: " + successor.getLength());
            }

            // Check for exactly one 'direction' tag
            NodeList predecessor = taskPrecedence.getElementsByTagName("predecessor");
            if (predecessor.getLength() != 1) {
            	throw new Exception("There must be exactly one 'predecessor' tag in TaskPrecedence at index: " + i + ", found: " + predecessor.getLength());            }
            
            NodeList workKind = taskPrecedence.getElementsByTagName("WorkSequenceKind");
            if (workKind.getLength() != 1) {
                throw new Exception("There must be exactly one 'WorkSequenceKind' tag in TaskPreceence at index: " + i + ", found: " + workKind.getLength());
            }
        }
    }


    private static void checkDirections(Document doc) throws Exception {
        NodeList taskParameters = doc.getElementsByTagName("TaskParameter");
        for (int i = 0; i < taskParameters.getLength(); i++) {
            Element taskParameter = (Element) taskParameters.item(i);
            NodeList directionNodes = taskParameter.getElementsByTagName("direction");
            if (directionNodes.getLength() == 0) {
                // If there is no <direction> tag at all
                throw new Exception("Missing direction tag in TaskParameter at index: " + i);
            }

            String direction = directionNodes.item(0).getTextContent().trim(); // Trim to avoid whitespace issues
            if (!(direction.equals("in") || direction.equals("out") || direction.equals("inout"))) {
                throw new Exception("Invalid direction value: '" + direction + "' in TaskParameter at index: " + i);
            }
        }
    }

    
    private static void checkKind(Document doc) throws Exception {
        NodeList taskPrecedences = doc.getElementsByTagName("TaskPrecedence");
        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element taskPrecedence = (Element) taskPrecedences.item(i);
            NodeList kindNodes = taskPrecedence.getElementsByTagName("WorkSequenceKind");
            
            // Check if the WorkSequenceKind tag exists within this TaskPrecedence
            //if (kindNodes.getLength() == 0 || kindNodes.item(0).getTextContent().trim().isEmpty()) {
             //   throw new Exception("Missing or empty WorkSequenceKind tag in TaskPrecedence at index: " + i);
            //}

            String WorkSequenceKind = kindNodes.item(0).getTextContent().trim(); // Remove any leading/trailing whitespace
            if (!(WorkSequenceKind.equals("FinishToStart") || WorkSequenceKind.equals("StartToFinish") ||
                  WorkSequenceKind.equals("FinishToFinish") || WorkSequenceKind.equals("StartToStart") || (WorkSequenceKind.equals("")))) {
                throw new Exception("Invalid WorkSequenceKind value: '" + WorkSequenceKind + "' in TaskPrecedence at index: " + i);
            }
        }
    }

    private static Set<String> gatherReferencableIds(Document doc) {
        Set<String> ids = new HashSet<>();
        // Collect all Task names
        NodeList tasks = doc.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            if (task.hasAttribute("name")) {
                ids.add(task.getAttribute("name"));
            }
        }
        // Collect all Product names
        NodeList products = doc.getElementsByTagName("Product");
        for (int i = 0; i < products.getLength(); i++) {
            Element product = (Element) products.item(i);
            if (product.hasAttribute("name")) {
                ids.add(product.getAttribute("name"));
            }
        }
        // Collect all Role names
        NodeList roles = doc.getElementsByTagName("Role");
        for (int i = 0; i < roles.getLength(); i++) {
            Element role = (Element) roles.item(i);
            if (role.hasAttribute("name")) {
                ids.add(role.getAttribute("name"));
            }
        }
        return ids;
    }

    private static void checkReferences(Document doc, Set<String> referencableIds) throws Exception {
        // Check all 'ref' attributes in the document to see if they refer to an existing id
        NodeList refElements = doc.getElementsByTagName("*");
        for (int i = 0; i < refElements.getLength(); i++) {
        	//System.out.println(refElements.getLength()); 
            Element element = (Element) refElements.item(i);
            if (element.hasAttribute("ref")) {
                String refValue = element.getAttribute("ref");
                if (!referencableIds.contains(refValue)) {
                    throw new Exception("Reference error:NO element with name '" + refValue + "' found in tag <" + element.getNodeName() + "> at index " + i);
                }
            }
        }
    }
}
