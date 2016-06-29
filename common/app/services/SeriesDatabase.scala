package services

import javax.inject.Singleton

import com.gu.contentapi.client.model.v1.{Tag, Content}
import model.SeriesUpdate

@Singleton
class SeriesDatabase {

  //Probably want this somewhere more manageable, like a database
  private val seriesToNotifyOn: List[String] = List(
    "artanddesign/series/art-weekly"
  )

  def tagsOfType(content: Content, tagType: String): List[Tag] = content.tags.filter(_.id == tagType).toList

  def maybeSeriesUpdateFor(content: Content): Option[SeriesUpdate] =
    content.tags.find(tag => seriesToNotifyOn.contains(tag.id)).map { tag =>
      SeriesUpdate(tag.id, tag.webTitle, content.id, content.webTitle)}

}