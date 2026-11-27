package ps.reso.instaeclipse.utils.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class StringTablesShapeTest {

    @Test
    public void staticIntToStringIsATable() {
        assertTrue(StringTables.isTableShape(Modifier.PUBLIC | Modifier.STATIC,
                Collections.singletonList("int"), "java.lang.String"));
    }

    @Test
    public void otherShapesAreNotTables() {
        int st = Modifier.PUBLIC | Modifier.STATIC;
        assertFalse(StringTables.isTableShape(Modifier.PUBLIC, Collections.singletonList("int"), "java.lang.String"));
        assertFalse(StringTables.isTableShape(st, Collections.singletonList("long"), "java.lang.String"));
        assertFalse(StringTables.isTableShape(st, Arrays.asList("int", "int"), "java.lang.String"));
        assertFalse(StringTables.isTableShape(st, Collections.singletonList("int"), "boolean"));
        assertFalse(StringTables.isTableShape(st, Collections.emptyList(), "java.lang.String"));
        assertFalse(StringTables.isTableShape(st, null, "java.lang.String"));
    }
}
