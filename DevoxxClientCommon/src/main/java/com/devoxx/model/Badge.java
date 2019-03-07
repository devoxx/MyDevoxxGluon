/**
 * Copyright (c) 2017, 2018 Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.model;

import java.util.Locale;
import java.util.Objects;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Badge extends Searchable {

    @SuppressWarnings("unused")
    public Badge() {
    }

//    public Badge(String qr) {
//        if (qr != null && ! qr.isEmpty() && qr.split("::").length == 8) {
//            String[] split = qr.split("::");
//            badgeId.set(split[0]);
//            firstName.set(split[1]);
//            lastName.set(split[2]);
//            role.set(split[3]);
//            programmingLanguages.set(split[4].replaceAll("\\|", ", "));
//            city.set(split[5]);
//            country.set(split[6]);
//            email.set(split[7]);
//        }
//    }

//    private StringProperty badgeId = new SimpleStringProperty();
//    public StringProperty badgeIdProperty() { return badgeId; }
//    public String getBadgeId() { return badgeId.get(); }
//    public void setBadgeId(String badgeId) { this.badgeId.set(badgeId); }

    private StringProperty name = new SimpleStringProperty();
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

//    private StringProperty lastName = new SimpleStringProperty();
//    public StringProperty lastNameProperty() { return lastName; }
//    public String getLastName() { return lastName.get(); }
//    public void setLastName(String lastName) { this.lastName.set(lastName); }

    private StringProperty jobTitle = new SimpleStringProperty();
    public StringProperty jobTitleProperty() { return jobTitle; }
    public String getJobTitle() { return jobTitle.get(); }
    public void setJobTitle(String jobTitle) {this.jobTitle.set(jobTitle); }

    private StringProperty language = new SimpleStringProperty();
    public StringProperty languageProperty() { return language; }
    public String getLanguage() { return language.get(); }
    public void setLanguage(String language) {this.language.set(language); }

    private IntegerProperty age = new SimpleIntegerProperty();
    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int age) {this.age.set(age); }

    private StringProperty gender = new SimpleStringProperty();
    public StringProperty genderProperty() { return gender; }
    public String getGender() { return gender.get(); }
    public void setGender(String gender) {this.gender.set(gender); }

    private StringProperty company = new SimpleStringProperty();
    public StringProperty companyProperty() { return company; }
    public String getCompany() { return company.get(); }
    public void setCompany(String company) {this.company.set(company); }

    private StringProperty programmingLanguages = new SimpleStringProperty();
    public StringProperty programmingLanguagesProperty() {return programmingLanguages;}
    public String getProgrammingLanguages() { return programmingLanguages.get(); }
    public void setProgrammingLanguages(String programmingLanguages) {this.programmingLanguages.set(programmingLanguages);}
    
    private StringProperty city = new SimpleStringProperty();
    public StringProperty cityProperty() {return city;}
    public String getCity() { return city.get(); }
    public void setCity(String city) {this.city.set(city);}
    
    private StringProperty country = new SimpleStringProperty();
    public StringProperty countryProperty() {return country;}
    public String getCountry() { return country.get(); }
    public void setCountry(String country) {this.country.set(country);}
    
    private StringProperty email = new SimpleStringProperty();
    public StringProperty emailProperty() { return email; }
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }

    private StringProperty details = new SimpleStringProperty();
    public StringProperty detailsProperty() { return details; }
    public String getDetails() { return details.get(); }
    public void setDetails(String details) { this.details.set(details); }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        } 
        final String lowerKeyword = keyword.toLowerCase(Locale.ROOT);

        return containsKeyword(getName(), lowerKeyword) ||
        	   containsKeyword(getEmail(), lowerKeyword) ||
        	   containsKeyword(getCompany(), lowerKeyword) ||
        	   containsKeyword(getCity(), lowerKeyword)   ||
        	   containsKeyword(getCountry(), lowerKeyword)   ||
        	   containsKeyword(getProgrammingLanguages(), lowerKeyword)   ||
               containsKeyword(getJobTitle(), lowerKeyword)   ||
               containsKeyword(getDetails(), lowerKeyword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Badge badge = (Badge) o;
        return Objects.equals(getEmail(), badge.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmail());
    }

    protected String safeStr(String s) {
        return s == null? "": s.trim();
    }
    
    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append(safeStr(getName()));
        csv.append(",").append(safeStr(getEmail()));
        csv.append(",").append(safeStr(getLanguage()));
        csv.append(",").append(safeStr(getAge()+""));
        csv.append(",").append(safeStr(getGender()));
        csv.append(",").append(safeStr(getCompany()));
        csv.append(",").append(safeStr(getCity()));        
        csv.append(",").append(safeStr(getCountry()));        
        csv.append(",").append(safeStr(getProgrammingLanguages().replaceAll(", ", "\\|")));
        csv.append(",").append(safeStr(getJobTitle()));
        csv.append(",").append(safeStr(getDetails()));
        return csv.toString();
    }

    public static Badge parseBadge(String qrCode) {
    	Badge badge = new Badge();
    	if (qrCode != null && !qrCode.trim().isEmpty()) {
    		String[] components = qrCode.split(";");
    		for (String component : components) {
				String[] keyValue = component.split(":");
				String key = keyValue[0].trim();
				String value = keyValue.length==2?keyValue[1].trim():"";
				switch (key.toLowerCase()) {
					case "name": badge.setName(value); break;
					case "email": badge.setEmail(value); break;
					case "language": badge.setLanguage(value); break;
					case "age": badge.setAge(value.isEmpty()?0:Integer.parseInt(value)); break;
					case "gender": badge.setGender(value); break;
					case "company": badge.setCompany(value); break;
					case "city" : badge.setCity(value); break;
					case "country": badge.setCountry(value); break;
					case "proglang": badge.setProgrammingLanguages(value); break;
					case "jobtitle": badge.setJobTitle(value); break;						
				}
			}
    	}
    	return badge;
    }
    
}
