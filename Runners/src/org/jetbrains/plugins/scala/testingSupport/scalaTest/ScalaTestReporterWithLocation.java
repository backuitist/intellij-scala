package org.jetbrains.plugins.scala.testingSupport.scalaTest;

import org.jetbrains.plugins.scala.testingSupport.TestRunnerUtil;
import org.scalatest.Reporter;
import org.scalatest.events.*;
import org.scalatest.events.Location;
import scala.Option;
import scala.Some;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import static org.jetbrains.plugins.scala.testingSupport.TestRunnerUtil.*;

/**
 * @author Alexander Podkhalyuzin
 */
public class ScalaTestReporterWithLocation implements Reporter {
  private String getStackTraceString(Throwable throwable) {
    StringWriter writer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(writer));
    return writer.getBuffer().toString();
  }

  private String getLocationHint(String className, Option<Location> locationOption, String testName) {
    if(locationOption instanceof Some) {
      Location location = locationOption.get();
      if(location instanceof TopOfClass)
        return " locationHint='scalatest://TopOfClass:" + ((TopOfClass) location).className() + "TestName:" + testName + "'";
      else if(location instanceof TopOfMethod) {
        TopOfMethod topOfMethod = (TopOfMethod) location;
        String methodId = topOfMethod.methodId();
        String methodName = methodId.substring(methodId.lastIndexOf('.') + 1, methodId.lastIndexOf('('));
        return " locationHint='scalatest://TopOfMethod:" + topOfMethod.className() + ":" + methodName + "TestName:" + testName + "'";
      }
      else if(location instanceof LineInFile) {
        LineInFile lineInFile = (LineInFile) location;
        return " locationHint='scalatest://LineInFile:" + className + ":" + lineInFile.fileName() +  ":" +
            lineInFile.lineNumber() + "TestName:" + testName + "'";
      }
      else
        return "";
    }
    else
      return "";
  }

  public void apply(Event event) {
    if (event instanceof RunStarting) {
      RunStarting r = (RunStarting) event;
      int testCount = r.testCount();
      System.out.println("##teamcity[testCount count='" + testCount + "']");
    } else if (event instanceof TestStarting) {
      TestStarting testStarting = ((TestStarting) event);
      String testText = testStarting.testText();
      String testName = testStarting.testName();
      String locationHint = getLocationHint(testStarting.suiteID(), testStarting.location(), testName);
      System.out.println("\n##teamcity[testStarted name='" + escapeString(testText) + "'" + locationHint +
          " captureStandardOutput='true']");
    } else if (event instanceof TestSucceeded) {
      Option<Object> durationOption = ((TestSucceeded) event).duration();
      long duration = 0;
      if (durationOption instanceof Some) {
        duration = ((java.lang.Long) durationOption.get()).longValue();
      }
      String testText = ((TestSucceeded) event).testText();
      System.out.println("\n##teamcity[testFinished name='" + escapeString(testText) +
          "' duration='"+ duration +"']");
    } else if (event instanceof TestFailed) {
      boolean error = true;
      Option<Throwable> throwableOption = ((TestFailed) event).throwable();
      String detail = "";
      if (throwableOption instanceof Some) {
        if (throwableOption.get() instanceof AssertionError) error = false;
        detail = getStackTraceString(throwableOption.get());
      }
      Option<Object> durationOption = ((TestFailed) event).duration();
      long duration = 0;
      if (durationOption instanceof Some) {
        duration = ((java.lang.Long) durationOption.get()).longValue();
      }
      String testText = ((TestFailed) event).testText();
      String message = ((TestFailed) event).message();
      long timeStamp = event.timeStamp();
      String res = "\n##teamcity[testFailed name='" + escapeString(testText) + "' message='" + escapeString(message) +
          "' details='" + escapeString(detail) + "'";
      if (error) res += "error = '" + error + "'";
      res += "timestamp='" + escapeString(formatTimestamp(new Date(timeStamp))) + "']";
      System.out.println(res);
      System.out.println("\n##teamcity[testFinished name='" + escapeString(testText) +
          "' duration='" + duration +"']");
    } else if (event instanceof TestIgnored) {
      String testText = ((TestIgnored) event).testText();
      System.out.println("\n##teamcity[testIgnored name='" + escapeString(testText) + "' message='" +
          escapeString("") + "']");
    } else if (event instanceof TestPending) {
      String testText = ((TestPending) event).testText();
      System.out.println("\n##teamcity[testFinished name='" + escapeString(testText) +
          "' duration='" + 0 +"']");
    } else if (event instanceof SuiteStarting) {
      SuiteStarting suiteStarting = (SuiteStarting) event;
      String suiteName = suiteStarting.suiteName();
      String locationHint = getLocationHint(suiteStarting.suiteID(), suiteStarting.location(), suiteName);
      System.out.println("\n##teamcity[testSuiteStarted name='" + escapeString(suiteName) + "'" + locationHint +
          " captureStandardOutput='true']");
    } else if (event instanceof SuiteCompleted) {
      String suiteName = ((SuiteCompleted) event).suiteName();
      System.out.println("\n##teamcity[testSuiteFinished name='" + escapeString(suiteName) + "']");
    } else if (event instanceof SuiteAborted) {
      String message = ((SuiteAborted) event).message();
      Option<Throwable> throwableOption = ((SuiteAborted) event).throwable();
      String throwableString = "";
      if (throwableOption instanceof Some) {
        throwableString = " errorDetails='" + escapeString(getStackTraceString(throwableOption.get())) + "'";
      }
      String statusText = "ERROR";
      String escapedMessage = escapeString(message);
      if (!escapedMessage.isEmpty()) {
        System.out.println("\n##teamcity[message text='" + escapedMessage + "' status='" + statusText + "'" +
            throwableString + "]");
      }
    } else if (event instanceof InfoProvided) {
      String message = ((InfoProvided) event).message();
      String escapedMessage = escapeString(message + "\n");
      if (!escapedMessage.isEmpty()) {
        System.out.println("\n##teamcity[message text='" + escapedMessage + "' status='WARNING'" + "]");
      }
    } else if (event instanceof RunStopped) {

    } else if (event instanceof RunAborted) {
      String message = ((RunAborted) event).message();
      Option<Throwable> throwableOption = ((RunAborted) event).throwable();
      String throwableString = "";
      if (throwableOption instanceof Some) {
        throwableString = " errorDetails='" + escapeString(getStackTraceString(throwableOption.get())) + "'";
      }
      String escapedMessage = escapeString(message);
      if (!escapedMessage.isEmpty()) {
        System.out.println("\n##teamcity[message text='" + escapedMessage + "' status='ERROR'" +
            throwableString + "]");
      }
    } else if (event instanceof RunCompleted) {

    }
    else if(event instanceof ScopeOpened) {
      ScopeOpened scopeOpened = (ScopeOpened) event;
      String message = scopeOpened.message();
      String locationHint = getLocationHint(scopeOpened.nameInfo().suiteID(), scopeOpened.location(), message);
      System.out.println("\n##teamcity[testSuiteStarted name='" + escapeString(message) + "'" + locationHint +
          " captureStandardOutput='true']");
    }
    else if(event instanceof ScopeClosed) {
      String message = ((ScopeClosed) event).message();
      System.out.println("\n##teamcity[testSuiteFinished name='" + escapeString(message) + "']");
    }
  }
}