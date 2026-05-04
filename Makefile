SHELL := /bin/bash

GRADLE := ./gradlew --no-daemon --console plain
MODULE := :compliance-reports:

.PHONY: help format license validate-format test java-test publish-public

help:
	@echo "Available targets: format license validate-format test java-test publish-public"

format license:
	$(GRADLE) $(MODULE)licenseFormat

validate-format:
	@echo "no format validation for gradle"

test java-test:
	$(GRADLE) $(MODULE)check $(MODULE)jacocoTestReport

publish-public:
	$(GRADLE) --info --stacktrace -DbuildNumber="${BUILD_NUMBER}" \
		$(MODULE)build $(MODULE)artifactoryPublish -x test
