package services

object Sentences {

  def reduceTo(sentencesInput: String, amountOfCharacters: Int): String =
    sentencesInput.take(amountOfCharacters).reverse.dropWhile(_ != ' ').reverse

  def reduceToWithEllipsis(sentencesInput: String, amountOfCharacters: Int): String = reduceTo(sentencesInput, amountOfCharacters) ++: "..."
}
