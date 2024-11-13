# Prettifier

## Overview

`Prettifier` is a Java command-line tool designed to beautify text files by converting airport codes into their full names or cities and formatting embedded dates and times. It reads an input text file, applies these replacements, and saves the formatted output to a specified file.

## Features

- Converts IATA (e.g., `#JFK`) and ICAO (e.g., `##EGLL`) airport codes to their respective airport names or cities.
- Formats date and time codes within the text using customizable formats.
- Cleans up unnecessary whitespace and handles special vertical whitespace characters.

### Installation Steps

1. **Clone the Repository:**
   ```bash
   git clone https://gitea.kood.tech/karlispaulus/itinerary
   ```
2. **Navigate to the Project Directory:**
    ```bash
    cd Prettifier
    ```
3. **Compile the Code:**
    ```bash
    javac Prettifier.java
    ```

### Usage Guide

1. **Run the application with the following command:**
    ```bash
    java Prettifier ./input.txt ./output.txt ./airport-lookup.csv
    ```
- `input.txt:` The file with unformatted text.
- `output.txt:` The file where formatted text will be saved.
- `airport-lookup.csv:` The file with airport codes and corresponding details for replacement.