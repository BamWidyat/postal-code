# Get Indonesian Postal Code

This is a clojure code made to get all the postal code from [kodepos.nomor.net](http://kodepos.nomor.net).

## Usage
1. Install latest version of Java
2. Install [leiningen](https://leiningen.org/).
3. Open terminal, move to folder directory and run this command
```
lein repl
```

## Functions

To get all the province number list use
```clj
(get-province-list)
```

To get all postal code from desired province use
```clj
(get-postcode-per-province <province-number>)
```

To get all Indonesian postal code use
```clj
(get-all-postcode)
```

The postal code will be exported as CSV file in the following directory
```
postal-code/src/
```
