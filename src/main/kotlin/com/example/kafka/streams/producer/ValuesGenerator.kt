/*
 * Copyright © 2020 - Monster Worldwide
 */

package com.example.kafka.streams.producer

import io.reactivex.Flowable
import io.smallrye.reactive.messaging.kafka.KafkaMessage
import io.smallrye.reactive.messaging.kafka.KafkaRecord
import org.eclipse.microprofile.reactive.messaging.Outgoing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.enterprise.context.ApplicationScoped


/**
 * A bean producing random temperature data every second.
 * The values are written to a Kafka topic (temperature-values).
 * Another topic contains the name of weather stations (weather-stations).
 * The Kafka configuration is specified in the application configuration.
 */
@ApplicationScoped
class ValuesGenerator {
    val LOG: Logger = LoggerFactory.getLogger(this.javaClass)

    private val random = Random()
    private val stations = listOf(
        WeatherStation(1, "Hamburg", 13),
        WeatherStation(2, "Snowdonia", 5),
        WeatherStation(3, "Boston", 11),
        WeatherStation(4, "Tokio", 16),
        WeatherStation(5, "Cusco", 12),
        WeatherStation(6, "Svalbard", -7),
        WeatherStation(7, "Porthsmouth", 11),
        WeatherStation(8, "Oslo", 7),
        WeatherStation(9, "Marrakesh", 20)
    )

    @Outgoing("temperature-values")
    fun generate(): Flowable<KafkaRecord<Int, String>> {
        return Flowable.interval(500, TimeUnit.MILLISECONDS)
            .onBackpressureDrop()
            .map {
                val station = stations[random.nextInt(stations.size)]
                val temperature: Double = BigDecimal.valueOf(random.nextGaussian() * 15 + station.averageTemperature)
                    .setScale(1, RoundingMode.HALF_UP)
                    .toDouble()
                LOG.info("station: {}, temperature: {}", station.name, temperature)
                KafkaRecord.of(station.id, Instant.now().toString() + ";" + temperature)
            }
    }

    @Outgoing("weather-stations")
    fun weatherStations(): Flowable<KafkaRecord<Int, String>> {
        val stationsAsJson: List<KafkaRecord<Int, String>> = stations.stream()
            .map { s: WeatherStation ->
                KafkaMessage.of(
                    s.id,
                    "{ \"id\" : ${s.id}, \"name\" : \"${s.name}\" }")
            }
            .collect(Collectors.toList())
        return Flowable.fromIterable(stationsAsJson)
    }

    private class WeatherStation(var id: Int, var name: String, var averageTemperature: Int)

}