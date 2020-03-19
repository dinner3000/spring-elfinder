package cn.kong.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import lombok.Getter;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractionXmlConfigBuilder {

    @Getter
    private Document document;
    private Element extractionsNode;

    public ExtractionXmlConfigBuilder() throws ParserConfigurationException {
        initDocument();

    }

    private void initDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        document = builder.newDocument();
        document.setXmlStandalone(true);

        // generate config header
        Element rootNode = document.createElement("root");
        document.appendChild(rootNode);
        rootNode.appendChild(createParametersNode());
        extractionsNode = document.createElement("Extractions");
        rootNode.appendChild(extractionsNode);
    }

    private Element createParametersNode() {
        Element parametersNode = document.createElement("Parameters");
        parametersNode.appendChild(createElementWithText( "TextQualifierYN", "Y"));
        parametersNode.appendChild(createElementWithText( "Delimiter", ","));
        parametersNode.appendChild(createElementWithText( "ConsiderBIParmsYN", "N"));
        return parametersNode;
    }

    public void appendExtractionNodeFromJsonObject(JSONObject jsonObject) {
        Element extractionNode = createElementWithAttribute("Extraction", "ExtractFileName", jsonObject.getString("ExtractFileName"));
        extractionNode.appendChild(createElementWithText("CompanyID", jsonObject.getString("CompanyID")));

        Element tablesNode = document.createElement("Tables");
        extractionNode.appendChild(tablesNode);

        // loop all table configs
        for (Object o : jsonObject.getJSONArray("Tables")) {
            JSONObject tableConfig = (JSONObject) o;

            Element tableNode = document.createElement("Table");
            tableNode.appendChild(createElementWithText("ModuleName", tableConfig.getString("ModuleName")));
            tableNode.appendChild(createElementWithText("TableName", tableConfig.getString("TableName")));

            Element columnsNode = document.createElement("Columns");
            for (Object p : tableConfig.getJSONArray("Columns")) {
                String columnName = (String) p;
                columnsNode.appendChild(createElementWithAttribute("Column", "FieldName", columnName));
            }
            tableNode.appendChild(columnsNode);

            Element filtersNode = document.createElement("Filters");
            for (Object q : tableConfig.getJSONArray("Filters")) {
                JSONObject filterConfig = (JSONObject) q;

                Element filterNode = document.createElement("Filter");
                filterNode.appendChild(createElementWithText("Column", wrapperQuotes(filterConfig.getString("Column"))));
                filterNode.appendChild(createNoEscapePrefix());
                filterNode.appendChild(createElementWithText("Operand", wrapperQuotes(filterConfig.getString("Operand"))));
                filterNode.appendChild(createNoEscapePostfix());
                filterNode.appendChild(createElementWithText("Value", wrapperQuotes(tryParseVariable(filterConfig.getString("Value")))));
                filtersNode.appendChild(filterNode);
            }
            tableNode.appendChild(filtersNode);

            tablesNode.appendChild(tableNode);
        }

        extractionsNode.appendChild(extractionNode);
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private String tryParseVariable(String val) {
        try {
            if (val.startsWith("$(") && val.endsWith(")")) {
                if (val.equals("$(PLACE_HOLDER)")) {
                    val = dateFormat.format(new Date());
                } else {
                    Pattern pattern = Pattern.compile("\\$\\((?<sign>[-+])?(?<num>\\d+)(?<unit>[dwmy])\\)");
                    Matcher matcher = pattern.matcher(val);
                    if (matcher.find()) {
                        int sign = matcher.group("sign").equals("-") ? -1 : 1;
                        int num = Integer.parseInt(matcher.group("num"));
                        String unit = matcher.group("unit");

                        Calendar calendar = Calendar.getInstance();
                        switch (unit) {
                            case "d":
                                calendar.add(Calendar.DAY_OF_MONTH, num * sign);
                                break;
                            case "w":
                                calendar.add(Calendar.WEEK_OF_YEAR, num * sign);
                                break;
                            case "m":
                                calendar.add(Calendar.MONTH, num * sign);
                                break;
                            case "y":
                                calendar.add(Calendar.YEAR, num * sign);
                                break;
                            default:
                                throw new RuntimeException("Invalid filter value format");
                        }
                        val = dateFormat.format(calendar.getTime());
                    }
                }
            }
            return val;
        } catch (Exception e) {
            throw new RuntimeException("Invalid filter value format", e);
        }
    }

    private String wrapperQuotes(String text) {
        return String.format("\"%s\"", text);
    }

    private Node createNoEscapePrefix() {
        return document.createProcessingInstruction(StreamResult.PI_DISABLE_OUTPUT_ESCAPING, "&");
    }

    private Node createNoEscapePostfix() {
        return document.createProcessingInstruction(StreamResult.PI_ENABLE_OUTPUT_ESCAPING, "&");
    }

    private Element createElementWithText(String tagName, String text) {
        Element element = document.createElement(tagName);
        element.setTextContent(text);
        return element;
    }

    private Element createElementWithAttribute(String tagName, String attrName, String attrValue) {
        Element element = document.createElement(tagName);
        element.setAttribute(attrName, attrValue);
        return element;
    }

}
