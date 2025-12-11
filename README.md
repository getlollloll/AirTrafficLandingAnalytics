# Air Traffic Landing Analytics

A Scala-based analytics system that analyzes San Francisco International Airport (SFO) landing data, forecasts future landing volumes, and detects anomalies.

## Project Overview

This project processes historical airline landing data to:
- Aggregate landing statistics by month and airline
- Calculate 6-month moving average baselines
- Forecast landing volumes for the next 6 months
- Detect anomalies when actual values deviate more than 30% from predictions

## Tech Stack

- **Scala 2.13**
- **Akka Typed** - Actor-based reactive architecture
- **TableParser** - Type-safe CSV parsing
- **ScalaTest + Akka TestKit** - Testing framework

## Data Source

- **Source:** San Francisco International Airport (SFO)
- **File:** `Air_Traffic_Landings_Statistics_20251118.csv`
- **Records:** 44,502 landing records
- **Time Span:** 1999 - 2025
- **Airlines:** 175 airlines

## Project Structure

```
airtrafficlanding/
├── src/main/scala/air/
│   ├── actors/           # Akka Typed Actors
│   │   ├── Guardian.scala
│   │   ├── LandingFetcher.scala
│   │   ├── LandingProcessor.scala
│   │   ├── LandingRecommender.scala
│   │   ├── RecordKeeper.scala
│   │   └── CsvWriter.scala
│   ├── app/              # Application entry point
│   │   └── Run.scala
│   ├── formats/          # Data parsing utilities
│   │   └── Formats.scala
│   ├── messages/         # Actor protocols
│   ├── model/            # Data models
│   │   └── LandingRecord.scala
│   └── service/          # Business logic
│       ├── BaselineCalculator.scala
│       ├── ForecastService.scala
│       ├── LandingReader.scala
│       └── LandingTableParser.scala
├── src/test/scala/air/   # Test files
├── output/               # Generated CSV files
├── lib/                  # TableParser JAR
└── build.sbt
```

## How to Run

1. Make sure you have SBT installed

2. Clone the repository
```bash
git clone [your-repo-url]
cd airtrafficlanding
```

3. Run the application
```bash
sbt run
```

4. Or specify a custom CSV path
```bash
sbt "run path/to/your/data.csv"
```

## Output Files

The system generates two CSV files in the `output/` folder:

### forecasts.csv
Predicted landing volumes for the next 6 months per airline.
```
airline,next_period,predicted_landings,predicted_weight_lb
```

### anomalies.csv
Detected anomalies where actual values deviate more than 30% from predictions.
```
airline,period,actual,predicted,deviation_percent
```


## Testing

Run all tests:
```bash
sbt test
```

Test coverage includes:
- BaselineCalculatorSpec - Monthly aggregation, moving average
- LandingForecasterSpec - Forecasting algorithm
- LandingReaderSpec - CSV parsing
- GuardianSpec, LandingFetcherSpec, LandingProcessorSpec - Actor behavior
- RecordKeeperSpec - Anomaly storage
- CsvWriterSpec - File output

## Architecture

The system uses a dual-path architecture:

**Batch Path:**
LandingReader → BaselineCalculator → ForecastService

**Reactive Path (Akka Actors):**
Guardian → LandingFetcher → LandingProcessor → LandingRecommender → RecordKeeper → CSV Output

## Course

CSYE 7200 - Big Data System Engineering

Northeastern University