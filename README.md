# SRE Demo

This repository is a very simple demo of a particular case for System
Reliability where the mean average shows higher than the 95th percentile
due to large outliers.  This demonstrates the oddity of when the mean
outweighs the percentiles

## Getting Started

Build the `server` component and `load-test` by running the following 
command.

```
mvn package
```

This will build the `server` Docker container and a simple load test
Docker container.  Normally, JMeter would be used for the load test
but this is meant to show simple coding exercise.

Once built, use Docker to start Influx, Grafana, the demo server, and
the load test.

```
docker-compose up
```

After a few seconds, the load test will kick in and start making HTTP
requests to the REST server.  The REST server will capture metrics and
send them to Influx.  Grafana will graph the metrics visually.

Visit http://localhost:3000/d/7xM8Y3Oik/sre-demo?refresh=5s to view the actual
dashboard.  The default password is `secret`.

![Dashboard](dashboard.png?raw=true "Dashboard")

## Understanding the Data

Metric values, particularly means and percentiles, result in what appears
often as oddities.  In this particular case, it should be expected that a
mean is generally closer to the median or the 50th percentile and as such
the 75th percentile should be much higher than the mean.  However, the load
and actual measurements can tell a different story.

First, means are highly different than percentiles as means tend to portray
impact of outliers.  The bigger the outlier, the more impactful the mean.
For example, a series of measurements such as 1,2,2,3,4,2,1,3,3,100 would 
have a median of 2, a 75th percentile of 3, yet of mean of 12.  Generally
speaking, when the mean and median are relatively close to each other, the 
distribution of data contains less outliers and when the mean is substantially
higher than the median, then either many outliers exist or very large outliers
exist.

Second, means within libraries such as Dropwizard that use exponentially decaying
weights, are not calculated purely as a mathematical mean dividing the total sum of
the measurements by the total count of measurements.  Instead, it weights each
measurement based on its recency and applies the weight to each measurement.
This causes newer measurements to have a stronger impact on the mean and older
measurements to have less of an impact.  In other words, on a service that 
is starting up will show an immediate increase in mean as all the high initial
measurements will have high weightings.  As the service continues, those high
outliers will become less impactful causing the lower times to take precedence
driving the mean down.  This is the whole point of the decaying measurements to
cause old measurements to decay to less value.  A quick example of three 
measurements shows this impact.  Consider the following three measurements
where T is the offset time (higher being more recent).

700ms @ T2
300ms @ T4
400ms @ T6

In this case, we end up with weights (2 of 12, 17%; 4 of 12, 33%; 6 of 12, 50%).
The statistical mean is (300+400+700)/3 or 467.  The weighted/decaying mean
is 700*.17 + 300*.33 + 400*.50 or 418 which is less than the mathematical mean
as old outliers have less impact.  If we switch the 700ms and 400ms to T6/T2, then
the 700ms outlier is most recent with the most impact.  This results in
700*.5 + 300*.33 + 400*.17 or 517.

Both of these reasons are why it is imperative that latency is understood not 
just from an average value, but also from the percentiles in order to compare
and understand the implications of their relationships. The percentiles work similar 
in that more recent data skews the percentiles more.

For this particular graph, we can see the average is well above the P75 and often
above the P99.  Immediately, this tells me that there are likely very large outliers
driving the mean higher.  In other words, 99% of the values are within a very small
range, but the 1% are way above that range.  Looking at the graph, the maximums are
75x larger than the P50.

The other interesting part of this graph is the P99 spikes where it drives well 
above the mean.  This is where the EDR comes into play.  At those particular times,
there were most likely more outliers.  Instead of 99% of values being in the small
range, it was likely 2-4% were out of range.

The final thing to note from this graph is that the traffic is very bursty from
very few requests to very large number of requests.  These spikes directly 
correlate to the maximum outliers.  Most likely, the service takes on much more
traffic resulting in higher CPU or memory resulting in more queueing causing 
response times to increase.  My guess is there is a scheduled process that hits
the service every minute with lots of requests.  During that minute, the mean
has a higher weight towards the increased response times.  The measurements then
decay until the next minute.  If in fact, this was a scheduled job running every
minute, I would probably recommend a way to spread the traffic out over the 60
seconds.  Instead of sending 1000 requests every minute, send 15 requests every
second to create a more uniform traffic distribution.

1.  Based on the throughput graph, what do you think traffic looks like to this service? Is it relatively constant or bursty? Regular or irregular?

As states above, the traffic is very bursty every minute.  This tends to be less
desired, but may be normal if understood and accounted for.  Ideally, more uniform
distribution of throughput is recommended to avoid sudden bursts that generally
impact many layers of the system.

2.  What kind of latency distribution might lead to a situation where percentile values (even those as high as the 99th) are generally lower than the mean?

Very large and few outliers often tend the skew the mean outside the standard distribution
of percentiles.  One large outlier out of 1000 will skew the mean considerably, but the 
P999 would be unimpacted.

3.  What kinds of conditions (either resource or semantic) might cause such a distribution in a Java application?

In the case above, external scheduled traffic or applications can lead to bursty traffic,
especially if it is the main driver of the overall traffic.  Within Java applications,
or any GC-based language, GC can lead to large outliers.  A service with a smaller eden
space that constantly overflows into old gen leading to old gen churn can cause large 
collection times leading to large spikes for a few requests.  This can easily occur even
on a service with normal traffic distribution.  Backend systems or networks can also
lead to queueing that can result in outliers.  Caching is another big suspect in identifying
outliers.  A service that has a 99% cache hit ratio may result in very low response
times.  The 1% cache misses could then lead to large response time outliers that again
drive the mean up.  Cache misses could be the result of periodic requests for old data
or from an automated system expiring the cache every minute.

4.  How might you design and train an anomaly detection algorithm for this distribution?

The first step in training any system is identifying the proper expectations.  A bursty
system or one with a few outliers may be perfectly acceptable in given situations.
While it may not be ideal, it may not be as impactful.  For example, an internal
tool that occassionally takes 30 seconds to load is less impactful than a credit card
transaction that takes 30 seconds to load.  Understanding these expectations will help
to know whether an outlier is an acceptable outlier or not.  Once expectations are met,
the good set of measurements can be used to train the data.  Rather than purely relying
on standard deviations to check which values are outside 3 or 4 deviations of the mean,
the acceptable distributions can be compared with the rolling window of the active
distributions.  These comparisons can then not only compare what are the standard 
measurements but also what and how often accepted outliers exist.  For this particular
graph, it may be expected to see large outlier maximums every minute.  However, if 
suddenly those outlier maximums double and occur every few seconds, then they would
be recognized as inconsistent with the trained model and be flagged as anomalies.
These models, should constantly be retrained and reanalyzed as traffic and services
are always evolving.
