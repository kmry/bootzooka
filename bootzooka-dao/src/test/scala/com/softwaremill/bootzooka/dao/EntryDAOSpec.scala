package com.softwaremill.bootzooka.dao

import com.softwaremill.bootzooka.domain.Entry
import org.bson.types.ObjectId
import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FlatSpec, BeforeAndAfterAll}

class MongoEntryDAOSpec extends FlatSpecWithMongo with EntryDAOSpec {
  behavior of "MongoEntryDAO"

  def createDAO = new MongoEntryDAO()
}

class InMemoryEntryDAOSpec extends EntryDAOSpec {
  behavior of "InMemoryEntryDAO"

  def createDAO = new InMemoryEntryDAO()
}

trait EntryDAOSpec extends FlatSpec with ShouldMatchers with BeforeAndAfterAll {
  def createDAO: EntryDAO

  var entryDAO: EntryDAO = _
  val referenceDate = new DateTime(DateTimeZone.UTC).withDate(2012, 12, 10)

  override def beforeAll() {
    super.beforeAll()
    entryDAO = createDAO
    for (i <- 1 to 3) {
      entryDAO.add(Entry(new ObjectId(i.toString * 24), "Message " + i, new ObjectId((10 - i).toString * 24), referenceDate.minusDays(i - 1)))
    }

    // one more entry for authorId "9" * 24
    entryDAO.add(Entry(new ObjectId("b" * 24), "Message 9", new ObjectId("9" * 24), referenceDate.minusDays(4)))
  }

  it should "count all entries" in {
    entryDAO.countItems() should be(4)
  }

  it should "load all entries" in {
    entryDAO.loadAll.size should be(4)
  }

  it should "load single entry" in {
    val entryOpt = entryDAO.load("1" * 24)
    entryOpt should be('defined)
  }

  it should "not find non existing entry" in {
    val entryOpt = entryDAO.load("19" * 12)
    entryOpt should be(None)
  }

  it should "add new entry" in {
    // Given
    val numberOfEntries = entryDAO.countItems()

    // When
    entryDAO.add(Entry(new ObjectId("a" * 24), "Message 9", new ObjectId("a" * 24), new DateTime()))

    // Then
    (entryDAO.countItems() - numberOfEntries) should be(1)
  }

  it should "remove entry" in {
    // Given
    val numberOfEntries = entryDAO.countItems()

    // When
    entryDAO.remove("a" * 24)

    // Then
    (entryDAO.countItems() - numberOfEntries) should be(-1)
  }

  it should "update existing entry" in {
    // Given
    val entryOpt: Option[Entry] = entryDAO.load("1" * 24)
    val message: String = "Updated message"

    // When
    entryOpt.foreach(e => entryDAO.update(e.id.toString, message))

    // Then
    val updatedEntryOpt: Option[Entry] = entryDAO.load("1" * 24)
    updatedEntryOpt match {
      case Some(e) => {
        e.text should be(message)
      }
      case _ => fail("Entry option should be defined")
    }
  }

  it should "load entries sorted from newest" in {
    // When
    val entries = entryDAO.loadAll

    // Then
    entries should have size (4)
    entries(0).entered.isAfter(entries(1).entered) should be(true)
    entries(1).entered.isAfter(entries(2).entered) should be(true)
  }

  it should "find 1 entry newer than given time" in {
    // Given
    val time = referenceDate.minusDays(1).getMillis

    // When
    val counter = entryDAO.countNewerThan(time)

    // Then
    counter should be(1)
  }

  it should "find 3 entries with time 1 ms before the oldest entry time" in {
    // Given
    val time = referenceDate.minusDays(3).minusMillis(1).getMillis

    // When
    val counter = entryDAO.countNewerThan(time)

    // Then
    counter should be(3)
  }

  it should "find no entries with time 1 mssec after the youngest entry time" in {
    // Given
    val time = referenceDate.plusMillis(1).getMillis

    // When
    val counter = entryDAO.countNewerThan(time)

    // Then
    counter should be(0)
  }

  it should "load entries created by given author" in {
    // Given
    val authorId = "9" * 24
    val authorObjectId = new ObjectId(authorId)

    // When
    val entries = entryDAO.loadAuthoredBy(authorId)

    // Then
    entries.size should be(2)
    entries.forall(_.authorId == authorObjectId) should be(true)
  }

  it should "find no entries for non existing author" in {
    // Given
    val nonExistingAuthorId = "5" * 24

    // When
    val entries = entryDAO.loadAuthoredBy(nonExistingAuthorId)

    // Then
    entries.size should be(0)
  }
}
