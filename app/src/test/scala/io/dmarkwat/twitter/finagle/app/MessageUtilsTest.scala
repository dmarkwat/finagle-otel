/*
 * This Scala source file was generated by the Gradle "init" task.
 */
package io.dmarkwat.twitter.finagle.app

import org.junit.{Assert, Test}

class MessageUtilsTest {
    @Test def testGetMessage(): Unit = {
        Assert.assertEquals("Hello      World!", MessageUtils.getMessage())
    }
}