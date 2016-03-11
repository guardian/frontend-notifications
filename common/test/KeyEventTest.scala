import model.KeyEvent
import org.scalatest.{Matchers, FreeSpec}

class KeyEventTest extends FreeSpec with Matchers {
  val keyEventOne = KeyEvent("1", None, "1")
  val keyEventTwo = KeyEvent("2", None, "2")
  val keyEventThree = KeyEvent("3", None, "3")

  "getLatestKeyEvents" - {
    "Give an empty list" in {
      KeyEvent.getLastestKeyEvents("non-exist", List()) should be (Nil)
    }

    "Drop the first event in" in {
      KeyEvent.getLastestKeyEvents("1", List(keyEventOne, keyEventTwo, keyEventThree)) should be (List(keyEventTwo, keyEventThree))
    }

    "Drop the first two events" in {
      KeyEvent.getLastestKeyEvents("2", List(keyEventOne, keyEventTwo, keyEventThree)) should be (List(keyEventThree))
    }

    "Drop all the events" in {
      KeyEvent.getLastestKeyEvents("3", List(keyEventOne, keyEventTwo, keyEventThree)) should be (Nil)
    }

    "Drop all the events for an event that hasn't been seen yet" in {
      KeyEvent.getLastestKeyEvents("unknown", List(keyEventOne, keyEventTwo, keyEventThree)) should be (Nil)
    }
  }
}
