package id.payu.simulator.dukcapil.service;

import id.payu.simulator.dukcapil.entity.Citizen;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.LocalDate;

/**
 * Initializes test citizen data on application startup.
 */
@ApplicationScoped
public class DataInitializer {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        Log.info("Initializing test citizen data...");

        if (Citizen.count() > 0) {
            Log.info("Test citizens already exist, skipping initialization");
            return;
        }

        // Valid citizens
        createCitizen("3201234567890001", "JOHN DOE", "JAKARTA", LocalDate.of(1990, 1, 15),
                Citizen.Gender.MALE, "O", "JL. SUDIRMAN NO. 123, RT 001/RW 002",
                "001", "002", "MENTENG", "MENTENG", "JAKARTA PUSAT", "DKI JAKARTA",
                Citizen.Religion.ISLAM, Citizen.MaritalStatus.MARRIED, "KARYAWAN SWASTA",
                Citizen.CitizenStatus.VALID);

        createCitizen("3201234567890002", "JANE DOE", "BANDUNG", LocalDate.of(1992, 5, 20),
                Citizen.Gender.FEMALE, "A", "JL. BRAGA NO. 456, RT 003/RW 004",
                "003", "004", "BRAGA", "SUMUR BANDUNG", "BANDUNG", "JAWA BARAT",
                Citizen.Religion.KRISTEN, Citizen.MaritalStatus.SINGLE, "DOKTER",
                Citizen.CitizenStatus.VALID);

        createCitizen("3201234567890004", "ALICE WONDERLAND", "SURABAYA", LocalDate.of(1988, 8, 8),
                Citizen.Gender.FEMALE, "B", "JL. TUNJUNGAN NO. 88",
                "005", "006", "GENTENG", "GENTENG", "SURABAYA", "JAWA TIMUR",
                Citizen.Religion.KATOLIK, Citizen.MaritalStatus.MARRIED, "PENGUSAHA",
                Citizen.CitizenStatus.VALID);

        createCitizen("3201234567890005", "BOB BUILDER", "SEMARANG", LocalDate.of(1985, 3, 25),
                Citizen.Gender.MALE, "AB", "JL. PANDANARAN NO. 101",
                "007", "008", "PEKUNDEN", "SEMARANG TENGAH", "SEMARANG", "JAWA TENGAH",
                Citizen.Religion.BUDDHA, Citizen.MaritalStatus.MARRIED, "KONTRAKTOR",
                Citizen.CitizenStatus.VALID);

        createCitizen("3201234567890006", "CHARLIE CHOCOLATE", "YOGYAKARTA", LocalDate.of(1995, 12, 25),
                Citizen.Gender.MALE, "O", "JL. MALIOBORO NO. 55",
                "009", "010", "NGUPASAN", "GONDOMANAN", "YOGYAKARTA", "DI YOGYAKARTA",
                Citizen.Religion.HINDU, Citizen.MaritalStatus.SINGLE, "PENGUSAHA",
                Citizen.CitizenStatus.VALID);

        // Blocked citizen
        createCitizen("3201234567890003", "BLOCKED USER", "SURABAYA", LocalDate.of(1985, 12, 1),
                Citizen.Gender.MALE, "B", "JL. TUNJUNGAN NO. 789",
                "010", "011", "EMBONG KALIASIN", "GENTENG", "SURABAYA", "JAWA TIMUR",
                Citizen.Religion.ISLAM, Citizen.MaritalStatus.DIVORCED, "TIDAK BEKERJA",
                Citizen.CitizenStatus.BLOCKED);

        // Invalid NIK (for testing)
        createCitizen("3299999999999999", "INVALID NIK TEST", "UNKNOWN", LocalDate.of(1900, 1, 1),
                Citizen.Gender.MALE, null, "UNKNOWN",
                null, null, null, null, null, null,
                null, null, null,
                Citizen.CitizenStatus.INVALID);

        // Deceased citizen
        createCitizen("3201234567890007", "DECEASED PERSON", "MEDAN", LocalDate.of(1950, 6, 15),
                Citizen.Gender.MALE, "A", "JL. ASIA NO. 1",
                "001", "001", "KESAWAN", "MEDAN BARAT", "MEDAN", "SUMATERA UTARA",
                Citizen.Religion.KONGHUCU, Citizen.MaritalStatus.WIDOWED, "PENSIUNAN",
                Citizen.CitizenStatus.DECEASED);

        Log.infof("Initialized %d test citizens", Citizen.count());
    }

    private void createCitizen(String nik, String fullName, String birthPlace, LocalDate birthDate,
                                Citizen.Gender gender, String bloodType, String address,
                                String rt, String rw, String village, String district,
                                String city, String province, Citizen.Religion religion,
                                Citizen.MaritalStatus maritalStatus, String occupation,
                                Citizen.CitizenStatus status) {
        Citizen citizen = new Citizen();
        citizen.nik = nik;
        citizen.fullName = fullName;
        citizen.birthPlace = birthPlace;
        citizen.birthDate = birthDate;
        citizen.gender = gender;
        citizen.bloodType = bloodType;
        citizen.address = address;
        citizen.rt = rt;
        citizen.rw = rw;
        citizen.village = village;
        citizen.district = district;
        citizen.city = city;
        citizen.province = province;
        citizen.religion = religion;
        citizen.maritalStatus = maritalStatus;
        citizen.occupation = occupation;
        citizen.status = status;
        citizen.persist();

        Log.debugf("Created citizen: %s - %s (%s)", nik, fullName, status);
    }
}
