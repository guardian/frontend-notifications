namespace scala com.gu.crier.model.event.v1

include "content/v1.thrift"

enum EventType {
    Update = 1,
    Delete = 2
}

enum ItemType {
    Content = 1,
    Tag = 2,
    Section = 3,
    Front = 4,
    StoryPackage = 5
}

struct Event {

    1: required EventType eventType

    2: required ItemType itemType

    3: required i64 dateTime

    4: required v1.Content content
}