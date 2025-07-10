package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.file_feature.common.exception.AudioMetaDataExtractionException;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.AudioMetadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.*;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class AudioMetadataExtractor {

    public static AudioMetadata extractMetadata(InputStream inputStream) {
        try {
            File file = inputStreamToTempFile(inputStream);
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            String title = tag.getFirst(FieldKey.TITLE);
            String artist = tag.getFirst(FieldKey.ARTIST);
            String genre = tag.getFirst(FieldKey.GENRE);
            int duration = audioFile.getAudioHeader().getTrackLength();

            byte[] coverArtBytes = Optional.ofNullable(tag.getFirstArtwork())
                    .map(Artwork::getBinaryData)
                    .orElse(null);

            String base64Cover = coverArtBytes != null
                    ? "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(coverArtBytes)
                    : null;

            return new AudioMetadata(title, artist, genre, duration, base64Cover);

        } catch (Exception e) {
            throw new AudioMetaDataExtractionException("Error while extracting metadata");
        }
    }

    private static File inputStreamToTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("audio_" + UUID.randomUUID(), ".mp3");
        try (OutputStream out = new FileOutputStream(tempFile)) {
            inputStream.transferTo(out);
        }
        return tempFile;
    }

}

