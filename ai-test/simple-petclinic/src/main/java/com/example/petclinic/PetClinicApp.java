package com.example.petclinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(
        name = "petclinic",
        mixinStandardHelpOptions = true,
        version = "simple-petclinic 1.0.0",
        description = "Simple Pet Clinic CLI",
        subcommands = {
                PetClinicApp.AddOwner.class,
                PetClinicApp.ListOwners.class,
                PetClinicApp.ShowOwner.class,
                PetClinicApp.AddPet.class,
                PetClinicApp.ListPets.class,
                PetClinicApp.AddVet.class,
                PetClinicApp.ListVets.class,
                PetClinicApp.AddVisit.class,
                PetClinicApp.ListVisits.class,
                PetClinicApp.Summary.class,
        })
public class PetClinicApp implements Runnable {

    public static final String DATA_FILE = "petclinic.json";

    public static void main(String[] args) {
        int code = new CommandLine(new PetClinicApp()).execute(args);
        System.exit(code);
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            Map<String, Object> data = new HashMap<>();
            data.put("owners", new ArrayList<Map<String, Object>>());
            data.put("pets", new ArrayList<Map<String, Object>>());
            data.put("vets", new ArrayList<Map<String, Object>>());
            data.put("visits", new ArrayList<Map<String, Object>>());
            data.put("nextId", 1);
            return data;
        }
        try {
            ObjectMapper m = new ObjectMapper();
            return m.readValue(f, Map.class);
        } catch (IOException e) {
            System.out.println("ERROR: cannot read " + DATA_FILE + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    static void saveData(Map<String, Object> data) {
        try {
            ObjectMapper m = new ObjectMapper();
            m.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), data);
        } catch (IOException e) {
            System.out.println("ERROR: cannot write " + DATA_FILE + ": " + e.getMessage());
            System.exit(1);
        }
    }

    static int nextId(Map<String, Object> data) {
        int id = ((Number) data.getOrDefault("nextId", 1)).intValue();
        data.put("nextId", id + 1);
        return id;
    }

    @Command(name = "add-owner", description = "Add an owner")
    static class AddOwner implements Runnable {
        @Option(names = "--first", required = true) String firstName;
        @Option(names = "--last", required = true) String lastName;
        @Option(names = "--address") String address;
        @Option(names = "--phone") String phone;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (firstName == null || firstName.trim().isEmpty()) {
                System.out.println("ERROR: first name required");
                System.exit(1);
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                System.out.println("ERROR: last name required");
                System.exit(1);
            }
            Map<String, Object> data = loadData();
            List<Map<String, Object>> owners = (List<Map<String, Object>>) data.get("owners");
            int id = nextId(data);
            Map<String, Object> owner = new HashMap<>();
            owner.put("id", id);
            owner.put("firstName", firstName);
            owner.put("lastName", lastName);
            owner.put("address", address == null ? "" : address);
            owner.put("phone", phone == null ? "" : phone);
            owners.add(owner);
            saveData(data);
            System.out.println("Added owner #" + id + ": " + firstName + " " + lastName);
        }
    }

    @Command(name = "list-owners", description = "List all owners")
    static class ListOwners implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> owners = (List<Map<String, Object>>) data.get("owners");
            if (owners.isEmpty()) {
                System.out.println("No owners.");
                return;
            }
            System.out.println("ID  | Name                     | Phone              | Address");
            System.out.println("----+--------------------------+--------------------+--------------------");
            for (Map<String, Object> o : owners) {
                String name = o.get("firstName") + " " + o.get("lastName");
                System.out.printf("%-3s | %-24s | %-18s | %s%n",
                        o.get("id"), name, o.get("phone"), o.get("address"));
            }
        }
    }

    @Command(name = "show-owner", description = "Show owner details with pets")
    static class ShowOwner implements Runnable {
        @Option(names = "--id", required = true) int id;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> owners = (List<Map<String, Object>>) data.get("owners");
            List<Map<String, Object>> pets = (List<Map<String, Object>>) data.get("pets");
            Map<String, Object> found = null;
            for (Map<String, Object> o : owners) {
                if (((Number) o.get("id")).intValue() == id) {
                    found = o;
                    break;
                }
            }
            if (found == null) {
                System.out.println("ERROR: owner not found: " + id);
                System.exit(1);
                return;
            }
            System.out.println("Owner #" + found.get("id") + ": " + found.get("firstName") + " " + found.get("lastName"));
            System.out.println("Address: " + found.get("address"));
            System.out.println("Phone:   " + found.get("phone"));
            System.out.println("Pets:");
            boolean any = false;
            for (Map<String, Object> p : pets) {
                if (((Number) p.get("ownerId")).intValue() == id) {
                    System.out.println("  - #" + p.get("id") + " " + p.get("name")
                            + " (" + p.get("type") + ", born " + p.get("birthDate") + ")");
                    any = true;
                }
            }
            if (!any) System.out.println("  (none)");
        }
    }

    @Command(name = "add-pet", description = "Add a pet to an owner")
    static class AddPet implements Runnable {
        @Option(names = "--owner-id", required = true) int ownerId;
        @Option(names = "--name", required = true) String name;
        @Option(names = "--birth", required = true, description = "Format: yyyy-MM-dd") String birthDate;
        @Option(names = "--type", required = true, description = "e.g. dog, cat, bird") String type;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (name == null || name.trim().isEmpty()) {
                System.out.println("ERROR: name required");
                System.exit(1);
            }
            try {
                new SimpleDateFormat("yyyy-MM-dd").parse(birthDate);
            } catch (Exception e) {
                System.out.println("ERROR: invalid date: " + birthDate + " (expected yyyy-MM-dd)");
                System.exit(1);
            }
            Map<String, Object> data = loadData();
            List<Map<String, Object>> owners = (List<Map<String, Object>>) data.get("owners");
            boolean ownerExists = false;
            for (Map<String, Object> o : owners) {
                if (((Number) o.get("id")).intValue() == ownerId) {
                    ownerExists = true;
                    break;
                }
            }
            if (!ownerExists) {
                System.out.println("ERROR: owner not found: " + ownerId);
                System.exit(1);
            }
            List<Map<String, Object>> pets = (List<Map<String, Object>>) data.get("pets");
            int id = nextId(data);
            Map<String, Object> pet = new HashMap<>();
            pet.put("id", id);
            pet.put("ownerId", ownerId);
            pet.put("name", name);
            pet.put("birthDate", birthDate);
            pet.put("type", type);
            pets.add(pet);
            saveData(data);
            System.out.println("Added pet #" + id + ": " + name + " (" + type + ") for owner #" + ownerId);
        }
    }

    @Command(name = "list-pets", description = "List all pets")
    static class ListPets implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> pets = (List<Map<String, Object>>) data.get("pets");
            if (pets.isEmpty()) {
                System.out.println("No pets.");
                return;
            }
            System.out.println("ID  | Name             | Type        | Birth      | Owner");
            System.out.println("----+------------------+-------------+------------+------");
            for (Map<String, Object> p : pets) {
                System.out.printf("%-3s | %-16s | %-11s | %-10s | %s%n",
                        p.get("id"), p.get("name"), p.get("type"), p.get("birthDate"), p.get("ownerId"));
            }
        }
    }

    @Command(name = "add-vet", description = "Add a vet")
    static class AddVet implements Runnable {
        @Option(names = "--first", required = true) String firstName;
        @Option(names = "--last", required = true) String lastName;
        @Option(names = "--spec", description = "Comma-separated specialties, e.g. surgery,dentistry") String spec;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (firstName == null || firstName.trim().isEmpty()) {
                System.out.println("ERROR: first name required");
                System.exit(1);
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                System.out.println("ERROR: last name required");
                System.exit(1);
            }
            Map<String, Object> data = loadData();
            List<Map<String, Object>> vets = (List<Map<String, Object>>) data.get("vets");
            int id = nextId(data);
            Map<String, Object> vet = new HashMap<>();
            vet.put("id", id);
            vet.put("firstName", firstName);
            vet.put("lastName", lastName);
            List<String> specs = new ArrayList<>();
            if (spec != null && !spec.trim().isEmpty()) {
                for (String s : spec.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) specs.add(trimmed);
                }
            }
            vet.put("specialties", specs);
            vets.add(vet);
            saveData(data);
            System.out.println("Added vet #" + id + ": " + firstName + " " + lastName);
        }
    }

    @Command(name = "list-vets", description = "List all vets")
    static class ListVets implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> vets = (List<Map<String, Object>>) data.get("vets");
            if (vets.isEmpty()) {
                System.out.println("No vets.");
                return;
            }
            System.out.println("ID  | Name                     | Specialties");
            System.out.println("----+--------------------------+--------------------");
            for (Map<String, Object> v : vets) {
                String name = v.get("firstName") + " " + v.get("lastName");
                List<String> specs = (List<String>) v.get("specialties");
                String specStr = (specs == null || specs.isEmpty()) ? "-" : String.join(", ", specs);
                System.out.printf("%-3s | %-24s | %s%n", v.get("id"), name, specStr);
            }
        }
    }

    @Command(name = "add-visit", description = "Add a visit")
    static class AddVisit implements Runnable {
        @Option(names = "--pet-id", required = true) int petId;
        @Option(names = "--vet-id", required = true) int vetId;
        @Option(names = "--date", required = true, description = "Format: yyyy-MM-dd") String date;
        @Option(names = "--desc", required = true) String description;
        @Option(names = "--cost", required = true) double cost;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                new SimpleDateFormat("yyyy-MM-dd").parse(date);
            } catch (Exception e) {
                System.out.println("ERROR: invalid date: " + date + " (expected yyyy-MM-dd)");
                System.exit(1);
            }
            if (cost < 0) {
                System.out.println("ERROR: cost cannot be negative");
                System.exit(1);
            }
            Map<String, Object> data = loadData();
            List<Map<String, Object>> pets = (List<Map<String, Object>>) data.get("pets");
            List<Map<String, Object>> vets = (List<Map<String, Object>>) data.get("vets");
            boolean petExists = false;
            for (Map<String, Object> p : pets) {
                if (((Number) p.get("id")).intValue() == petId) {
                    petExists = true;
                    break;
                }
            }
            if (!petExists) {
                System.out.println("ERROR: pet not found: " + petId);
                System.exit(1);
            }
            boolean vetExists = false;
            for (Map<String, Object> v : vets) {
                if (((Number) v.get("id")).intValue() == vetId) {
                    vetExists = true;
                    break;
                }
            }
            if (!vetExists) {
                System.out.println("ERROR: vet not found: " + vetId);
                System.exit(1);
            }
            List<Map<String, Object>> visits = (List<Map<String, Object>>) data.get("visits");
            int id = nextId(data);
            Map<String, Object> visit = new HashMap<>();
            visit.put("id", id);
            visit.put("petId", petId);
            visit.put("vetId", vetId);
            visit.put("date", date);
            visit.put("description", description);
            visit.put("cost", cost);
            visits.add(visit);
            saveData(data);
            System.out.printf("Added visit #%d for pet #%d on %s (cost: %.2f)%n", id, petId, date, cost);
        }
    }

    @Command(name = "list-visits", description = "List visits, optionally filtered by pet")
    static class ListVisits implements Runnable {
        @Option(names = "--pet-id") Integer petId;

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> visits = (List<Map<String, Object>>) data.get("visits");
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> v : visits) {
                if (petId == null || ((Number) v.get("petId")).intValue() == petId) {
                    filtered.add(v);
                }
            }
            if (filtered.isEmpty()) {
                System.out.println("No visits.");
                return;
            }
            System.out.println("ID  | Date       | Pet | Vet | Cost     | Description");
            System.out.println("----+------------+-----+-----+----------+-------------");
            double total = 0;
            for (Map<String, Object> v : filtered) {
                double c = ((Number) v.get("cost")).doubleValue();
                total += c;
                System.out.printf("%-3s | %-10s | %-3s | %-3s | %8.2f | %s%n",
                        v.get("id"), v.get("date"), v.get("petId"), v.get("vetId"), c, v.get("description"));
            }
            System.out.println("----+------------+-----+-----+----------+-------------");
            System.out.printf("Total: %.2f (%d visits)%n", total, filtered.size());
        }
    }

    @Command(name = "summary", description = "Print clinic summary")
    static class Summary implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Map<String, Object> data = loadData();
            List<Map<String, Object>> owners = (List<Map<String, Object>>) data.get("owners");
            List<Map<String, Object>> pets = (List<Map<String, Object>>) data.get("pets");
            List<Map<String, Object>> vets = (List<Map<String, Object>>) data.get("vets");
            List<Map<String, Object>> visits = (List<Map<String, Object>>) data.get("visits");
            double totalRevenue = 0;
            for (Map<String, Object> v : visits) {
                totalRevenue += ((Number) v.get("cost")).doubleValue();
            }
            System.out.println("=== Pet Clinic Summary ===");
            System.out.println("Owners:  " + owners.size());
            System.out.println("Pets:    " + pets.size());
            System.out.println("Vets:    " + vets.size());
            System.out.println("Visits:  " + visits.size());
            System.out.printf("Revenue: %.2f%n", totalRevenue);
        }
    }
}
