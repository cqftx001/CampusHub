package com.campushub.bootstrap;

import com.campushub.test.ModuleBoundaryArchitectureTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.campushub", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureVerificationTest extends ModuleBoundaryArchitectureTest {
}
