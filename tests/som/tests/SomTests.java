/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package som.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Builder;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;

import som.interpreter.SomLanguage;
import som.vm.Universe;
import som.vmobjects.SObject;


@RunWith(Parameterized.class)
public class SomTests {

  @Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Array"},
        {"Block"},
        {"ClassLoading"},
        {"ClassStructure"},

        {"Closure"},
        {"Coercion"},
        {"CompilerReturn"},
        {"DoesNotUnderstand"},
        {"Double"},

        {"Empty"},
        {"Global"},
        {"Hash"},
        {"Integer"},

        {"Preliminary"},
        {"Reflection"},
        {"SelfBlock"},
        {"SpecialSelectorsTest"},
        {"Super"},

        {"Set"},
        {"String"},
        {"Symbol"},
        {"System"},
        {"Vector"}
    });
  }

  private String testName;

  public SomTests(final String testName) {
    this.testName = testName;
  }

  @Test
  public void testSomeTest() {
    Builder builder = PolyglotEngine.newBuilder();
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.VM_ARGS,
        new String[] {"-cp", "Smalltalk", "TestSuite/TestHarness.som", testName});

    PolyglotEngine engine = builder.build();
    Value returnCode = engine.eval(SomLanguage.START);

    Universe universe = engine.findGlobalSymbol(SomLanguage.UNIVERSE).as(Universe.class);
    Object obj = returnCode.as(DynamicObject.class);
    if (obj instanceof DynamicObject) {
      assertEquals("System",
          universe.sclass.getName(SObject.getSOMClass((DynamicObject) obj)).getString());
    } else {
      assertEquals(0, (int) returnCode.as(Integer.class));
    }
  }
}
