package com.debangshu.dedupe.generator;

import java.util.List;

/**
 * Word lists for the test data generator. All companies are made up.
 * The lists are short on purpose: with 40,000 people and ~1,750 name combinations, many DIFFERENT people
 * share a name ("Rahul Sharma" at three companies). Those are the hard cases that must NOT be merged.
 */
final class SampleData {

    static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Aditi", "Akash", "Amit", "Ananya", "Anjali", "Ankit", "Arjun", "Ayesha", "Deepak",
            "Divya", "Farhan", "Gaurav", "Imran", "Isha", "Karan", "Kavya", "Manish", "Meera", "Neha",
            "Nikhil", "Pooja", "Priya", "Rahul", "Rajesh", "Ritu", "Rohan", "Sanjay", "Sara", "Shreya",
            "Sneha", "Suresh", "Tanvi", "Varun", "Vikram", "Vivek", "Yash", "Zoya", "John", "Michael",
            "Sarah", "Emily", "David", "Daniel", "Laura", "James", "Maria", "Carlos", "Wei", "Yuki");

    static final List<String> LAST_NAMES = List.of(
            "Sharma", "Verma", "Gupta", "Patel", "Shah", "Mehta", "Iyer", "Nair", "Reddy", "Rao",
            "Kumar", "Singh", "Das", "Bose", "Chatterjee", "Banerjee", "Mukherjee", "Joshi", "Kulkarni", "Desai",
            "Pillai", "Menon", "Agarwal", "Khan", "Ali", "Fernandes", "D'Souza", "Smith", "Johnson", "Brown",
            "Garcia", "Lee", "Chen", "Tanaka", "Müller");

    static final List<String> COMPANIES = List.of(
            "Bluepeak Software", "Nimbus Analytics", "Zenith Logistics", "Orbit Retail", "Sahyadri Foods",
            "Kaveri Textiles", "Trident Health", "Lotus Fintech", "Indus Cloud Systems", "Ganga Pharma",
            "Peacock Media", "Monsoon Travels", "Vertex Robotics", "Saffron Hotels", "Granite Builders",
            "Coral Insurance", "Banyan Learning", "Quartz Labs", "Harbor Shipping", "Neem Organics",
            "Falcon Security", "Maple Consulting", "Silverline Telecom", "Aravalli Mining", "Brightpath Education",
            "Cobalt Energy", "Deccan Motors", "Evergreen Agro", "Garuda Aviation", "Himalaya Water",
            "Ivory Interiors", "Jade Jewellers", "Kestrel Drones", "Lumen Diagnostics", "Mango Payments",
            "Nova Biotech", "Opal Fashion", "Pinnacle Realty", "Quill Publishing", "Riverstone Capital",
            "Summit Sports", "Teal Logistics", "Velvet Cosmetics", "Willow Healthcare", "Zephyr Airlines");

    /** The same company is typed with many different legal suffixes in a real CRM. */
    static final List<String> COMPANY_SUFFIXES = List.of(
            "Pvt Ltd", "Pvt. Ltd.", "Private Limited", "Ltd", "Limited", "Inc", "Inc.", "LLC", "");

    static final List<String> CITIES = List.of(
            "Bengaluru", "Mumbai", "Pune", "Hyderabad", "Chennai", "Delhi", "Gurugram", "Noida", "Kolkata",
            "Ahmedabad", "Jaipur", "Kochi", "London", "Singapore", "Austin");

    static final List<String> PERSONAL_DOMAINS = List.of(
            "gmail.com", "gmail.com", "gmail.com", "gmail.com", "gmail.com", "gmail.com",
            "yahoo.co.in", "outlook.com", "outlook.com", "rediffmail.com");

    private SampleData() {
    }
}
