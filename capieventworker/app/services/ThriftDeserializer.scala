package services

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TIOStreamTransport
import scala.util.Try
import com.gu.crier.model.event.v1.Event

trait ThriftDeserializer {
  def deserializeEvent(buffer: ByteBuffer): Try[Event] = {
    Try {
      val bis = new ByteArrayInputStream(buffer.array());
      val transport = new TIOStreamTransport(bis)
      val protocol = new TCompactProtocol(transport)

      Event.decode(protocol)
    }
  }
}

object ThriftDeserializer extends ThriftDeserializer
