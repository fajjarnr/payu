package id.payu.simulator.dukcapil.service;

import id.payu.simulator.dukcapil.entity.Citizen;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import id.payu.simulator.dukcapil.entity.CitizenStatus;
import id.payu.simulator.dukcapil.entity.Gender;
import id.payu.simulator.dukcapil.entity.MaritalStatus;
import id.payu.simulator.dukcapil.entity.Religion;

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
                Gender.MALE, "O", "JL. SUDIRMAN NO. 123, RT 001/RW 002",
                "001", "002", "MENTENG", "MENTENG", "JAKARTA PUSAT", "DKI JAKARTA",
                Religion.ISLAM, MaritalStatus.MARRIED, "KARYAWAN SWASTA",
                CitizenStatus.VALID);

        createCitizen("3201234567890002", "JANE DOE", "BANDUNG", LocalDate.of(1992, 5, 20),
                Gender.FEMALE, "A", "JL. BRAGA NO. 456, RT 003/RW 004",
                "003", "004", "BRAGA", "SUMUR BANDUNG", "BANDUNG", "JAWA BARAT",
                Religion.KRISTEN, MaritalStatus.SINGLE, "DOKTER",
                CitizenStatus.VALID);

        createCitizen("3201234567890004", "ALICE WONDERLAND", "SURABAYA", LocalDate.of(1988, 8, 8),
                Gender.FEMALE, "B", "JL. TUNJUNGAN NO. 88",
                "005", "006", "GENTENG", "GENTENG", "SURABAYA", "JAWA TIMUR",
                Religion.KATOLIK, MaritalStatus.MARRIED, "PENGUSAHA",
                CitizenStatus.VALID);

        createCitizen("3201234567890005", "BOB BUILDER", "SEMARANG", LocalDate.of(1985, 3, 25),
                Gender.MALE, "AB", "JL. PANDANARAN NO. 101",
                "007", "008", "PEKUNDEN", "SEMARANG TENGAH", "SEMARANG", "JAWA TENGAH",
                Religion.BUDDHA, MaritalStatus.MARRIED, "KONTRAKTOR",
                CitizenStatus.VALID);

        createCitizen("3201234567890006", "CHARLIE CHOCOLATE", "YOGYAKARTA", LocalDate.of(1995, 12, 25),
                Gender.MALE, "O", "JL. MALIOBORO NO. 55",
                "009", "010", "NGUPASAN", "GONDOMANAN", "YOGYAKARTA", "DI YOGYAKARTA",
                Religion.HINDU, MaritalStatus.SINGLE, "PENGUSAHA",
                CitizenStatus.VALID);

        // Blocked citizen
        createCitizen("3201234567890003", "BLOCKED USER", "SURABAYA", LocalDate.of(1985, 12, 1),
                Gender.MALE, "B", "JL. TUNJUNGAN NO. 789",
                "010", "011", "EMBONG KALIASIN", "GENTENG", "SURABAYA", "JAWA TIMUR",
                Religion.ISLAM, MaritalStatus.DIVORCED, "TIDAK BEKERJA",
                CitizenStatus.BLOCKED);

        // Invalid NIK (for testing)
        createCitizen("3299999999999999", "INVALID NIK TEST", "UNKNOWN", LocalDate.of(1900, 1, 1),
                Gender.MALE, null, "UNKNOWN",
                null, null, null, null, null, null,
                null, null, null,
                CitizenStatus.INVALID);

        // Deceased citizen
        createCitizen("3201234567890007", "DECEASED PERSON", "MEDAN", LocalDate.of(1950, 6, 15),
                Gender.MALE, "A", "JL. ASIA NO. 1",
                "001", "001", "KESAWAN", "MEDAN BARAT", "MEDAN", "SUMATERA UTARA",
                Religion.KONGHUCU, MaritalStatus.WIDOWED, "PENSIUNAN",
                CitizenStatus.DECEASED);

        Log.infof("Initialized %d test citizens", Citizen.count());
    }

    private void createCitizen(String nik, String fullName, String birthPlace, LocalDate birthDate,
                                Gender gender, String bloodType, String address,
                                String rt, String rw, String village, String district,
                                String city, String province, Religion religion,
                                MaritalStatus maritalStatus, String occupation,
                                CitizenStatus status) {
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
