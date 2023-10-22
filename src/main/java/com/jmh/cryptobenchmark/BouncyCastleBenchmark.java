package com.jmh.cryptobenchmark;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;


public class BouncyCastleBenchmark {

    private static int COMPRESSION_ALGO = CompressionAlgorithmTags.ZIP;
    private static int SYMMETRIC_KEY_ALGO = SymmetricKeyAlgorithmTags.AES_128;
    private static boolean ARMOR = true;
    private static boolean WITH_INTEGRITY_CHECK = true;
    private static int BUFFER_SIZE = 1 << 16;

    @Benchmark
    @Fork(value = 1, jvmArgs = {"-Xms3G", "-Xmx7G"})
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 5, time = 5)
    @BenchmarkMode(Mode.AverageTime)
    public static int _1_Encrypt() throws IOException, PGPException {
        // public key
        String publicKeyFileName = "public-key.txt";
        PGPPublicKey publicKey = readPublicKey(publicKeyFileName);

        // encrypt
        encryptFile(new File("data.txt"), publicKey, new File("encrypted.txt"));

        return 0;
    }

    @Benchmark
    @Fork(value = 1, jvmArgs = {"-Xms3G", "-Xmx7G"})
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 5, time = 5)
    @BenchmarkMode(Mode.AverageTime)
    public static int _2_Decrypt() throws IOException, PGPException {
        // file names
        String privateKeyFileName = "private-key.txt";
        String password = "test"; // password of pgp key

        // decrypt
        findPrivateKeyAndDecryptFile(new File("encrypted.txt"), new File(privateKeyFileName), password, new File("decrypted.txt"));

        return 0;
    }

    public static void encryptFile(File inputFile, PGPPublicKey publicKey, File outputFile) throws IOException, PGPException {
        Security.addProvider(new BouncyCastleProvider());
        PGPEncryptedDataGenerator pgpEncryptedDataGenerator = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(SYMMETRIC_KEY_ALGO)
                        .setWithIntegrityPacket(WITH_INTEGRITY_CHECK)
                        .setSecureRandom(new SecureRandom())
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME));
        pgpEncryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey));

        OutputStream outputStream = new FileOutputStream(outputFile);
        if (ARMOR) {
            outputStream = new ArmoredOutputStream(outputStream);
        }

        OutputStream encryptedOutputStream = pgpEncryptedDataGenerator.open(outputStream, new byte[BUFFER_SIZE]);

        // compression
        PGPCompressedDataGenerator pgpCompressedDataGenerator = new PGPCompressedDataGenerator(COMPRESSION_ALGO);
        OutputStream compressedOutputStream = pgpCompressedDataGenerator.open(encryptedOutputStream, new byte[BUFFER_SIZE]);

        // encryption
        PGPLiteralDataGenerator pgpLiteralDataGenerator = new PGPLiteralDataGenerator();
        OutputStream literalDataOutStream = pgpLiteralDataGenerator.open(compressedOutputStream, PGPLiteralData.BINARY, inputFile);

        byte[] bytes = IOUtils.toByteArray(new FileInputStream(inputFile));

        literalDataOutStream.write(bytes);
        literalDataOutStream.close();
        pgpLiteralDataGenerator.close();
        compressedOutputStream.close();
        pgpEncryptedDataGenerator.close();
        outputStream.close();
    }

    public static void findPrivateKeyAndDecryptFile(File inputFile, File privateKeyFile, String password, File outputFile) throws IOException, PGPException {
        Security.addProvider(new BouncyCastleProvider());
        InputStream inputStream = new FileInputStream(inputFile);
        inputStream = PGPUtil.getDecoderStream(inputStream);
        OutputStream outputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        // first object might be a PGP market packet
        PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(inputStream, new JcaKeyFingerprintCalculator());
        PGPEncryptedDataList pgpEncryptedDataList;
        Object object = pgpObjectFactory.nextObject();
        if (object instanceof PGPEncryptedDataList) {
            pgpEncryptedDataList = (PGPEncryptedDataList) object;
        } else {
            pgpEncryptedDataList = (PGPEncryptedDataList) pgpObjectFactory.nextObject();
        }

        // find secret key
        Iterator<PGPEncryptedData> pgpPubKeyEncDataIterator = pgpEncryptedDataList.getEncryptedDataObjects();
        PGPPrivateKey privateKey = null;
        PGPPublicKeyEncryptedData pubKeyEncData = null;
        while (privateKey == null && pgpPubKeyEncDataIterator.hasNext()) {
            pubKeyEncData = (PGPPublicKeyEncryptedData) pgpPubKeyEncDataIterator.next();
            privateKey = findPrivateKey(new FileInputStream(privateKeyFile), pubKeyEncData.getKeyID(), password.toCharArray());
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("secret ket for message not found");
        }

        decryptFile(pubKeyEncData, privateKey, bufferedOutputStream);
    }

    static void decryptFile(PGPPublicKeyEncryptedData encryptedData, PGPPrivateKey privateKey, OutputStream outputStream) throws PGPException, IOException {
        PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(privateKey);
        InputStream decryptedCompressedInStr = encryptedData.getDataStream(decryptorFactory);

        JcaPGPObjectFactory decCompObjFac = new JcaPGPObjectFactory(decryptedCompressedInStr);
        PGPCompressedData pgpCompressedData = (PGPCompressedData) decCompObjFac.nextObject();

        InputStream compressedDataStream = new BufferedInputStream(pgpCompressedData.getDataStream());
        JcaPGPObjectFactory pgpCompObjFac = new JcaPGPObjectFactory(compressedDataStream);

        Object message = pgpCompObjFac.nextObject();

        if (message instanceof PGPLiteralData) {
            PGPLiteralData literalData = (PGPLiteralData) message;
            InputStream decDataStream = literalData.getInputStream();
            IOUtils.copy(decDataStream, outputStream);
            outputStream.close();
        } else if (message instanceof PGPOnePassSignatureList) {
            throw new PGPException("enc message contains a signed message not literal data");
        } else {
            throw new PGPException("message is not a simple encrypted file");
        }

        // perf integrity check
        if (encryptedData.isIntegrityProtected() && !encryptedData.verify()) {
            throw new PGPException("message failed integrity check");
        }
    }



    static PGPPrivateKey findPrivateKey(InputStream keyFileStream, long keyId, char[] password) throws IOException, PGPException {
        PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyFileStream), new JcaKeyFingerprintCalculator());
        PGPSecretKey secretKey = pgpSecretKeyRingCollection.getSecretKey(keyId);
        if (secretKey == null) {
            return null;
        }
        return secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(password));
    }

    static PGPPublicKey readPublicKey(String fileName) throws IOException, PGPException {
        InputStream keyIn = new BufferedInputStream(new FileInputStream(fileName));
        PGPPublicKey pubKey = readPublicKey(keyIn);
        keyIn.close();
        return pubKey;
    }

    static PGPPublicKey readPublicKey(InputStream input) throws IOException, PGPException {
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //

        Iterator keyRingIter = pgpPub.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

            Iterator keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) keyIter.next();

                if (key.isEncryptionKey()) {
                    return key;
                }
            }
        }

        throw new IllegalArgumentException("Can't find encryption key in key ring.");
    }

}

