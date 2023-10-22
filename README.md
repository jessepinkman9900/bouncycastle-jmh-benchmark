# bouncycastle-jmh-benchmark
bouncycastle benchmark

## Benchmarks Performed
- Local 
  - Config
    - 1GB file
    - default JVM settings
  - [Results](./benchmark_results/local_1GB_benchmark.txt)
    ```js
    Result "com.jmh.cryptobenchmark.BouncyCastleBenchmark._1_Encrypt":
      8.922 ±(99.9%) 2.441 s/op [Average]
      (min, avg, max) = (8.158, 8.922, 9.591), stdev = 0.634
      CI (99.9%): [6.481, 11.363] (assumes normal distribution)
    
    Result "com.jmh.cryptobenchmark.BouncyCastleBenchmark._2_Decrypt":
      0.876 ±(99.9%) 0.069 s/op [Average]
      (min, avg, max) = (0.845, 0.876, 0.889), stdev = 0.018
      CI (99.9%): [0.806, 0.945] (assumes normal distribution)
      
    Benchmark                         Mode  Cnt  Score   Error  Units
    BouncyCastleBenchmark._1_Encrypt  avgt    5  8.922 ± 2.441   s/op
    BouncyCastleBenchmark._2_Decrypt  avgt    5  0.876 ± 0.069   s/op
    ```
- Docker 1GB file
  - Config
    - 1GB file
    - default JVM settings
  - [Results](./benchmark_results/docker_1GB_benchmark.txt)
    ```js
    Result "com.jmh.cryptobenchmark.BouncyCastleBenchmark._1_Encrypt":
      5.835 ±(99.9%) 2.221 s/op [Average]
      (min, avg, max) = (5.208, 5.835, 6.415), stdev = 0.577
      CI (99.9%): [3.614, 8.055] (assumes normal distribution)
      
    Result "com.jmh.cryptobenchmark.BouncyCastleBenchmark._2_Decrypt":
      1.592 ±(99.9%) 0.167 s/op [Average]
      (min, avg, max) = (1.529, 1.592, 1.629), stdev = 0.043
      CI (99.9%): [1.425, 1.759] (assumes normal distribution)
      
    Benchmark                         Mode  Cnt  Score   Error  Units
    BouncyCastleBenchmark._1_Encrypt  avgt    5  5.835 ± 2.221   s/op
    BouncyCastleBenchmark._2_Decrypt  avgt    5  1.592 ± 0.167   s/op
    ```

## Sample Run
### Local
```sh
# generate key
gpg --full-gen-key

# export public key
gpg --output public-key.txt --export --armor <signature>

# export private key
gpg --output private-ket.txt  --armor --export-secret-keys <signature>

# generate 1 Gb file
sh generate-data.sh 1000000000

# run benchmark
./mvnw test > local_benchmark.txt
```

### Docker
```sh
# generate key
gpg --full-gen-key

# export public key
gpg --output public-key.txt --export --armor <signature>

# export private key
gpg --output private-ket.txt  --armor --export-secret-keys <signature>

# generate 1 Gb file
sh generate-data.sh 1000000000

# build docker image
docker build -t local-crypto-bench:0.0.1 .

# run docker image
docker run local-crypto-bench:0.0.1 > docker_benchmark.txt
```
## Details
### Run Benchmark
- need to increase heap size for file larger than 1GB
  - JVM Configs: `-Xms2G -Xmx8G`
  - heap settings configured in `pom.xml`
```sh
./mvnw test
```

### Running in docker
- @Fork value must be 0 in order to run in docker [stack answer](https://stackoverflow.com/a/75758302)
- Dockerfile needs these files in class-path
  - `data.txt`
  - `public-key.txt`
  - `private-key.txt`

## Misc
### Generate 200MB File
```sh
# generate 200MB file called data.txt
sh generate-data.sh 200000000 
```

### Working with PGP keys
```sh
# generate key
gpg --full-gen-key

# list public keys
gpg --list-keys

# export public key
gpg --output public-key.txt --export --armor <signature>

# list private keys
gpg --list-secret-keys

# export private key
 gpg --output priv-ket.txt  --armor --export-secret-keys <signature>
```

## Resources Used
- https://github.com/theautonomy/bouncycastle-gpg-example
- https://github.com/keith0591/pgp-encryption
