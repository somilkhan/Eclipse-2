package io.github.somilkhan.instaeclipse2.utils.backup;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/** SEC-07: the clone trust store must stay out of Auto Backup and device transfer. */
public class BackupRulesTest {

    private static File xml(String name) {
        File f = new File("src/main/res/xml/" + name);
        if (!f.exists()) f = new File("app/src/main/res/xml/" + name); // run from the repo root
        assertTrue(f.getAbsolutePath(), f.exists());
        return f;
    }

    private static boolean excludes(Element section, String domain, String path) {
        NodeList ex = section.getElementsByTagName("exclude");
        for (int i = 0; i < ex.getLength(); i++) {
            Element e = (Element) ex.item(i);
            if (domain.equals(e.getAttribute("domain")) && path.equals(e.getAttribute("path"))) return true;
        }
        return false;
    }

    private static Document parse(String name) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml(name));
    }

    @Test public void fullBackupContentExcludesCloneTrust() throws Exception {
        assertTrue(excludes(parse("backup_rules.xml").getDocumentElement(), "sharedpref", "eclipse2_clone_trust.xml"));
    }

    @Test public void dataExtractionRulesExcludeCloneTrustForCloudAndDeviceTransfer() throws Exception {
        Document d = parse("data_extraction_rules.xml");
        for (String section : new String[]{"cloud-backup", "device-transfer"}) {
            NodeList n = d.getElementsByTagName(section);
            assertEquals(section, 1, n.getLength());
            assertTrue(section, excludes((Element) n.item(0), "sharedpref", "eclipse2_clone_trust.xml"));
        }
    }
}
