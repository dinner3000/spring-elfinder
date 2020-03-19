package cn.kong.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.*;

@SpringBootTest
public class ExtractionXmlConfigBuilderTest {

    @Test
    public void test() throws ParserConfigurationException, TransformerException, IOException {
        File tableConfigDir = new File(Objects.requireNonNull(
                ExtractionXmlConfigBuilderTest.class.getClassLoader().getResource("static/files/extraction-config/table-config")).getPath());
        assert tableConfigDir.isDirectory();
        ExtractionXmlConfigBuilder xmlConfigBuilder = new ExtractionXmlConfigBuilder();
        FilenameFilter filenameFilter = new SuffixFileFilter(".json", IOCase.INSENSITIVE);
        for (File f : Objects.requireNonNull(tableConfigDir.listFiles(filenameFilter))) {
            JSONObject jsonObject = JSONObject.parseObject(new FileInputStream(f.getPath()), StandardCharsets.UTF_8, Object.class);
            xmlConfigBuilder.appendExtractionNodeFromJsonObject(jsonObject);
        }

        ExtractionXmlConfigWriter.write(xmlConfigBuilder.getDocument(), "c:/Temp/test.xml");
    }
}