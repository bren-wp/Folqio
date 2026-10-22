"""Regression tests for conservative Android dead-code auditing."""
import unittest
from scripts.audit_source import check_unique_imports_and_types, scan_symbols


class SourceAuditTests(unittest.TestCase):
    def test_private_unreferenced_function_is_only_advisory(self):
        source = {
            "Example.kt": "private fun unused() = Unit\nfun used() { }\n"
        }
        self.assertTrue(any("unused" in issue for issue in scan_symbols(source)))

    def test_private_function_called_elsewhere_is_not_flagged(self):
        source = {
            "Example.kt": "private fun doWork() = Unit\nfun call() = doWork()\n"
        }
        self.assertFalse(any("doWork" in issue for issue in scan_symbols(source)))

    def test_unused_import_advisory(self):
        source = {"Example.kt": "import java.io.File\nfun ping() = Unit\n"}
        self.assertTrue(any("java.io.File" in issue for issue in scan_symbols(source)))

    def test_duplicate_imports_are_a_failure(self):
        issues = []
        check_unique_imports_and_types(
            {"Example.kt": "import java.io.File\nimport java.io.File\nclass One {}\n"},
            issues
        )
        self.assertTrue(any("duplicate imports" in issue for issue in issues))

    def test_duplicate_top_level_type_is_a_failure(self):
        issues = []
        check_unique_imports_and_types(
            {"Example.kt": "class One {}\nclass One {}\n"}, issues
        )
        self.assertTrue(any("duplicated top-level type" in issue for issue in issues))

    def test_overloaded_methods_not_mistaken_for_duplicate_types(self):
        issues = []
        check_unique_imports_and_types(
            {"Example.kt": "class One {\nfun read(x: Int) {}\nfun read(x: String) {}\n}\n"},
            issues
        )
        self.assertFalse(issues)


if __name__ == "__main__":
    unittest.main()
