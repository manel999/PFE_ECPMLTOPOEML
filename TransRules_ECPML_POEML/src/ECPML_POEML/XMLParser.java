package ECPML_POEML;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class XMLParser {

    public static void main(String[] args) {
        try {
            // Map containing tag replacements
           
            // Define the base directory containing folders with XML files
            String baseDir = "C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML";

            // Walk through each file in each directory under baseDir
            Files.walk(Paths.get(baseDir))
                 .filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".xml"))  // Ensure only XML files are processed
                 .filter(path -> !path.getFileName().toString().equalsIgnoreCase("output.xml"))
                 .forEach(path -> {
                     System.out.println("Processing file: " + path.getFileName()); // Print the file name
                     processFile(path.toFile()); // Process the file
                 });
            
           
            //System.out.println("XML file" + outputFile+"verified");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Element findTaskByName(Document document, String taskName) {
        NodeList taskNodes = document.getElementsByTagName("Task");
        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node taskNode = taskNodes.item(i);
            if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
                Element taskElement = (Element) taskNode;
                String nameAttribute = taskElement.getAttribute("name");
                if (nameAttribute.equals(taskName)) {
                    return taskElement;
                }
            }
        }
        return null;
    }
    
    private static void processFile(File inputFile) {
        try {
        	
        	 Map<String, String> tagReplacements = new HashMap<>();
             // Définition des remplacements de tags à effectuer lors de la transformation XML
                tagReplacements.put("taskParameters", "TaskParameters");
                tagReplacements.put("taskParameter", "TaskParameter");
                tagReplacements.put("direction", "direction");
                tagReplacements.put("linkedTask", "LinkedTask");
                tagReplacements.put("product", "product");
                tagReplacements.put("taskPerformer", "TaskPerformer");
                tagReplacements.put("Role", "Role");
                tagReplacements.put("linkKind", "WorkSequenceKind");
                tagReplacements.put("successor", "successor");
                tagReplacements.put("predecessor", "predecessor");
                tagReplacements.put("linkToPredecessor", "TaskPrecedence");
                tagReplacements.put("linkToSuccessor", "TaskPrecedence");
                //tagReplacements.put("LinkTosuccessors", "TaskPrecedences");
                //tagReplacements.put("LinkToPredecessors", "TaskPrecedences");
                tagReplacements.put("workProduct", "Product");
                tagReplacements.put("nestedProduct", "Aggregation");
                tagReplacements.put("nestedProducts", "Aggregations");
                tagReplacements.put("impactedProduct", "ImpactedProduct");
                tagReplacements.put("workProducts", "Products");
                tagReplacements.put("linkToSuccessors", "WorkSequences");
                tagReplacements.put("linkToPredecessors", "WorkSequences");
                tagReplacements.put("taskPerformers", "TaskPerformers");
                tagReplacements.put("impactedProducts", "ImpactedProducts");
                tagReplacements.put("Tasks", "Tasks");
                tagReplacements.put("Task", "Task");
                tagReplacements.put("ECPMLModel", "POEMLModel");

            File outputFile = new File(inputFile.getParent(), "output.xml");  // Save the output file in the same directory

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputFile);

            //Map<String, String> tagReplacements = initializeTagReplacements();
            performTagReplacements(document.getDocumentElement(), tagReplacements);
            wrapAllTaskPrecedences(document);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(outputFile));

            System.out.println("Processed file saved to " + outputFile.getPath());
            
            verifPOEML.validateXML(outputFile);
            
        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getPath());
            e.printStackTrace();
        }
    }

    
    private static void wrapAllTaskPrecedences(Document document) {
        NodeList tasks = document.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            NodeList taskPrecedences = task.getElementsByTagName("TaskPrecedence");
            if (taskPrecedences.getLength() > 0) {
                Element taskPrecedencesElement = document.createElement("TaskPrecedences");
                while (taskPrecedences.getLength() > 0) {
                    Element tp = (Element) taskPrecedences.item(0);
                    taskPrecedencesElement.appendChild(tp);
                }
                task.appendChild(taskPrecedencesElement);
            }
        }
    }
    private static void performTagReplacements(Element element, Map<String, String> tagReplacements) {
        String tagName = element.getTagName();
        if (!tagName.equals("toolDefinition") && tagReplacements.containsKey(tagName)) {
            Document document = element.getOwnerDocument();
            Element newElement = document.createElement(tagReplacements.get(tagName));

            if (tagName.equals("taskPerformer")) {
                newElement.setAttribute("Type", "Performer");
            }
            
            NodeList useNodes = element.getElementsByTagName("use");
            for (int i = 0; i < useNodes.getLength(); i++) {
                Node useNode = useNodes.item(i);
                if (useNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element useElement = (Element) useNode;
                    String toolRef = useElement.getElementsByTagName("tool").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    String referencedTaskName = useElement.getElementsByTagName("managedTask").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    Element referencedTaskElement = findTaskByName(document, referencedTaskName);
                    if (referencedTaskElement != null) {
                        referencedTaskElement.setAttribute("Description", toolRef);
                    }
                }
            }
            
            if (tagName.equals("Task") && element.getAttribute("type").equals("CollaborativeTask")) {
            	NamedNodeMap attributes1 = element.getAttributes();
            	for (int i = 0; i < attributes1.getLength(); i++) {
            	    Attr attr = (Attr) attributes1.item(i);
            	    // Exclude the 'type' attribute from copying
            	    if (!attr.getName().equals("type")) {
            	        newElement.setAttribute(attr.getName(), attr.getValue());
            	    }
            	}
                NodeList taskPerformerNodes = element.getElementsByTagName("taskPerformer");
                int numberOfTaskPerformers = taskPerformerNodes.getLength();
                System.out.println(numberOfTaskPerformers);
                Element sousTasksElement = document.createElement("sous-tasks");
                for (int i = 0; i < taskPerformerNodes.getLength(); i++) {
                    Node taskPerformerNode = taskPerformerNodes.item(i);
                    if (taskPerformerNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element sousTaskElement = document.createElement("sous-task");
                        sousTaskElement.setAttribute("name", element.getAttribute("name") + " instance " + (i + 1));
                        System.out.println(i);
                        System.out.println("Name attribute of sous-task element: " + sousTaskElement.getAttribute("name"));
                        NamedNodeMap attributes = element.getAttributes();
                        for (int j = 0; j < attributes.getLength(); j++) {
                            Attr attr = (Attr) attributes.item(j);
                            if (!attr.getName().equals("type")) {
                                element.setAttribute(attr.getName(), attr.getValue());
                            }
                        }
                     
                        NodeList childNodes = element.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node childNode = childNodes.item(j);
                            String childTagName = (childNode instanceof Element) ? ((Element) childNode).getTagName() : null;
                            if (!(childTagName != null && childTagName.equals("taskPerformers"))) {
                                Node importedNode = document.importNode(childNode, true);
                                sousTaskElement.appendChild(importedNode);
                            }
                        }
                        Element taskPerformerElement = (Element) taskPerformerNode;
                        Element performerElement = document.createElement("taskPerformer");
                        NodeList performerChildren = taskPerformerElement.getChildNodes();
                        for (int k = 0; k < performerChildren.getLength(); k++) {
                            Node performerChild = performerChildren.item(k);
                            if (performerChild instanceof Element) {
                                Node importedNode = document.importNode(performerChild, true);
                                performerElement.appendChild(importedNode);
                            }
                        }
                        
                        sousTaskElement.appendChild(performerElement);
                        sousTasksElement.appendChild(sousTaskElement);
                    }
                }
                newElement.appendChild(sousTasksElement);
            } else {
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Attr attr = (Attr) attributes.item(i);
                    if (!tagName.equals("Task") || !attr.getName().equals("type")) {
                        newElement.setAttribute(attr.getName(), attr.getValue());
                    }
                    //newElement.setAttribute(attr.getName(), attr.getValue());
                }
             // Copier les enfants de l'ancien élément vers le nouveau
                NodeList childNodes = element.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    String childTagName = (childNode instanceof Element) ? ((Element) childNode).getTagName() : null;
                    if (!(childTagName != null && (childTagName.equals("toolDefinition") || childTagName.equals("toolsDefinition") || childTagName.equals("use") || childTagName.equals("uses")))) {
                        Node importedNode = document.importNode(childNode, true);
                        newElement.appendChild(importedNode);
                    }
                }
            }

         // Remplacer l'ancien élément par le nouveau dans le document
            element.getParentNode().replaceChild(newElement, element);
            element = newElement;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                performTagReplacements((Element) child, tagReplacements);
            }
        }
    }

}