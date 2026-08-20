package unq.pdes.flightsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlightsServiceApplication

fun main(args: Array<String>) {
    runApplication<FlightsServiceApplication>(*args)
}
