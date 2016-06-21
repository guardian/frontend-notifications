package services

import org.scalatest.{Matchers, FreeSpec}

class SentencesTest extends FreeSpec with Matchers {
  val sentences: Seq[String] = Seq(
    "Amy Cartel Land The Look of Silence What Happened, Miss Simone? Winter on Fire: Ukraine’s Fight for Freedom ",
    "Well, Real finish with a 12th straight victory, but Barcelona have wrapped up a 3-0 victory at Granada, and the title is theirs. Congratulations to Luis Enrique and his team! ",
    "That’s the whistle, and it’s official: Real finish second in La Liga ",
    "46 min: Change for Real Madrid: James Rodriguez in for Cristiano Ronaldo. He’s got a week to prepare for the Champions League final, but also: the pichichi is Suarez’s. ",
    "Adam McKay – The Big Short George Miller – Mad Max: Fury Road Alejandro González Iñárritu – The Revenant Lenny Abrahamson – Room Tom McCarthy – Spotlight "
  )

  val sentencesWithSpecificEndings: Seq[(String, String, String)] = Seq(
    (
      "Deportivo: Real finish",
      "Deportivo: Real ",
      "Deportivo: Real ..."
      ),
    (
      "Tense moments in Spain ",
      "Tense moments in Spain ",
      "Tense moments in Spain ..."
      ),
    (
      "Another day,",
      "Another ",
      "Another ..."
      )
  )

  "Sentences reduceTo" - {
    "should not touch the sentences" in {
      sentences.foreach { sentence =>
        Sentences.reduceTo(sentence, 412) should be (sentence)
      }
    }

    "should reduce correctly down" in {
      sentencesWithSpecificEndings.foreach { case (sentence, sentenceReducedToThree, _) =>
        Sentences.reduceTo(sentence, 412) should be (sentenceReducedToThree)
      }
    }

    "should reduce correctly down with added ellipsis" in {
      sentencesWithSpecificEndings.foreach { case (sentence, _, sentenceWithEllipsis) =>
        Sentences.reduceToWithEllipsis(sentence, 412) should be (sentenceWithEllipsis)
      }
    }
  }
}
