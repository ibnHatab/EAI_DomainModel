package domain.model

import java.util.Date;

trait Identity(id: Int)

trait Entity(val id: Int) extends Identity(id)

trait DomainEvent(val eventVersion: Int, val occurredOn: Date)

trait EventSourcedRoot {
  def accept(event: DomainEvent)
}

trait ValueObject extends AnyRef
