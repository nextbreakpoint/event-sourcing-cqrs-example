package com.nextbreakpoint.blueprint.common.vertx;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Codec {
    public static <T> byte[] asBytes(Class<T> clazz, T record) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var datumWriter = new SpecificDatumWriter<>(clazz);
            var encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            datumWriter.write(record, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Can't encode record", e);
        }
    }

    public static <T> T fromBytes(Class<T> clazz, byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            var datumReader = new SpecificDatumReader<>(clazz);
            var decoder = DecoderFactory.get().directBinaryDecoder(inputStream, null);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Can't decode data", e);
        }
    }

    public static String asString(Class<? extends SpecificRecord> clazz, SpecificRecord record) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var datumWriter = new SpecificDatumWriter(clazz);
            var schema = record.getSchema();
            var encoder = EncoderFactory.get().jsonEncoder(schema, outputStream, false);
            datumWriter.write(record, encoder);
            encoder.flush();
            return outputStream.toString();
        } catch (IOException e) {
            throw new RuntimeException("Can't encode record", e);
        }
    }

    public static String asString(Class<? extends SpecificRecord> clazz, Object record) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var datumWriter = new SpecificDatumWriter(clazz);
            var schema = clazz.getDeclaredConstructor().newInstance().getSchema();
            var encoder = EncoderFactory.get().jsonEncoder(schema, outputStream, false);
            datumWriter.write(record, encoder);
            encoder.flush();
            return outputStream.toString();
        } catch (IOException | InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Can't encode record", e);
        }
    }

    public static <T extends SpecificRecord> T fromString(Class<T> clazz, String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes())) {
            var datumReader = new SpecificDatumReader<>(clazz);
            var schema = clazz.getDeclaredConstructor().newInstance().getSchema();
            var decoder = DecoderFactory.get().jsonDecoder(schema, inputStream);
            return datumReader.read(null, decoder);
        } catch (IOException | InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Can't decode data", e);
        }
    }
}
