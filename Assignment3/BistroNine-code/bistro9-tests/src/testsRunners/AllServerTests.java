package testsRunners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("testsServer") // <--- This string must match your package name exactly
public class AllServerTests {
    // This class remains empty
}