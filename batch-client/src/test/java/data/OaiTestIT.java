package data;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class OaiTestIT {

    /**
     * This test iterates over OAI identifiers from Metis test repository and checks if all the identifiers
     * are 50 letters long. It uses Metis library to do that and https as a communication protocol.
     * <p>
     * This test should <b>FAIL</b> after several iterations because of the bug described in
     * <a href="https://europeana.atlassian.net/browse/MET-6076">MET-6076</a>
     * </p>
     *
     *
     * @throws IOException
     * @throws HarvesterException
     */
    @Test
    void runListIdentifiersOverHttpsUsingMetisLib() throws IOException, HarvesterException {
        final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
        OaiHarvest oaiHarvest = new OaiHarvest("https://metis-repository-rest.test.eanadev.org/repository/oai",
                "edm", "Heide1000elements");

        int iterationCount = 0;
        while (true) {
            System.out.println("Iteration " + ++iterationCount);

            try (HarvestingIterator<OaiRecordHeader, ?> headerIterator = oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
                headerIterator.forEach(oaiRecordHeader -> {
                    if (oaiRecordHeader.getOaiIdentifier().length() != 50) {
                        System.out.println("ERROR_1: " + oaiRecordHeader.getOaiIdentifier());
                        Assertions.fail("Incorrect identifier found");
                    }
                    return ReportingIteration.IterationResult.CONTINUE;
                });
            }
        }
    }

    /**
     * This test iterates over OAI identifiers from Metis test repository and checks if all the identifiers
     * are 50 letters long. It uses Metis library to do that and http as a communication protocol.
     * <p>
     * This test should <b> NEVER FAIL</b>.
     * </p>
     * <p>
     *     The only one difference comparing to previous test is the communication protocol.
     *     Here we have HTTP, in the previous test we had HTTPS.
     * </p>
     *
     *
     * @throws IOException
     * @throws HarvesterException
     */
    @Test
    void runListIdentifiersOverHttpUsingMetisLib() throws IOException, HarvesterException {
        final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
        OaiHarvest oaiHarvest = new OaiHarvest("http://metis-repository-rest.test.eanadev.org/repository/oai",
                "edm", "Heide1000elements");

        int iterationCount = 0;
        while (true) {
            System.out.println("Iteration " + ++iterationCount);
            try (HarvestingIterator<OaiRecordHeader, ?> headerIterator = oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
                headerIterator.forEach(oaiRecordHeader -> {
                    if (oaiRecordHeader.getOaiIdentifier().length() != 50) {
                        System.out.println("ERROR_1: " + oaiRecordHeader.getOaiIdentifier());
                        Assertions.fail("Incorrect identifier found");
                    }
                    return ReportingIteration.IterationResult.CONTINUE;
                });
            }
        }
    }

    /**
     * This test iterates over OAI identifiers from Metis test repository and checks if
     * response contains valid XML all the identifiers are 50 letters long.
     * It uses default http client for communication with repository and HTTP protocol.
     * <p>
     * This test should <b> NEVER FAIL</b>.
     * </p>
     *
     * @throws IOException
     * @throws HarvesterException
     */
    @Test
    void runListIdentifiersOverHttpsUsingHandMadeHttpClient() throws URISyntaxException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        int iterationCount = 0;
        while (true) {
            System.out.println("Iteration " + ++iterationCount);
            String identifiers = sendListIdentifiersRequest("https://metis-repository-rest.test.eanadev.org/repository/oai?verb=ListIdentifiers&metadataPrefix=edm&set=Heide1000elements");
            validateListIdentifiersResponse(identifiers);
        }
    }

    /**
     * This test iterates over OAI identifiers from Metis test repository and checks if
     * response contains valid XML all the identifiers are 50 letters long.
     * It uses default http client for communication with repository and HTTPS protocol.
     * <p>
     * This test should <b> NEVER FAIL</b>.
     * </p>
     *
     * @throws IOException
     * @throws HarvesterException
     */
    @Test
    void runListIdentifiersOverHttpUsingHandMadeHttpClient() throws URISyntaxException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        int iterationCount = 0;
        while (true) {
            System.out.println("Iteration " + ++iterationCount);
            String identifiers = sendListIdentifiersRequest("http://metis-repository-rest.test.eanadev.org/repository/oai?verb=ListIdentifiers&metadataPrefix=edm&set=Heide1000elements");
            validateListIdentifiersResponse(identifiers);
        }
    }


    private String sendListIdentifiersRequest(String uri) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .build();

        HttpClient client = HttpClient
                .newBuilder()
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());


        client.close();
        return response.body();
    }

    private void validateListIdentifiersResponse(String listIdentifiers) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new StringReader(listIdentifiers)));
        NodeList identifier = doc.getElementsByTagName("identifier");
        int length1 = identifier.getLength();
        for (int i = 0; i < length1; i++) {
            Node item = identifier.item(i);
            String textContent = item.getTextContent();
            if (textContent.length() != 50) {
                Assertions.fail("Incorrect identifier found: " + textContent);
            }
        }
        System.out.println("xml valid");
    }

}
