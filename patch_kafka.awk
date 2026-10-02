/^  kafka:/ {
    in_kafka = 1
    print
    next
}
in_kafka && /^    volumes:/ {
    print "    healthcheck:"
    print "      test: [\"CMD\", \"kafka-broker-api-versions\", \"--bootstrap-server\", \"localhost:9092\"]"
    print "      interval: 10s"
    print "      timeout: 5s"
    print "      retries: 5"
    print "      start_period: 20s"
    print $0
    in_kafka = 0
    next
}
{ print }
