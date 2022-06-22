/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.flyway;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SampleFlywayApplicationTests {
	@Autowired private JdbcTemplate template;

	@Autowired private PersonRepository personRepository;

	@Autowired private PersonController personController;

	@Test
	public void testDefaultSettings() throws Exception {
		assertEquals(new Integer(1), this.template
				.queryForObject("SELECT COUNT(*) from person", Integer.class));
		Person person = personRepository.findAll().iterator().next();
		assertEquals(person.getFirstName(), "Dave");
		assertEquals(person.getSurname(), "Syer");
	}

	@Test
	public void should_have_the_last_name_column_still_present_in_db() throws Exception {
		assertEquals(new Integer(1), this.template
				.queryForObject("SELECT COUNT(last_name) from person", Integer.class));
	}

	@Test
	public void should_instert_data_only_to_new_column() throws Exception {
		personController.generatePerson();
		personController.generatePerson();
		personController.generatePerson();

		Iterable<Person> persons = personRepository.findAll();

		for (Person person : persons) {
			assertNotNull(person.getFirstName());
			assertNotNull(person.getSurname());
			if (!"Syer".equals(person.getSurname())) {
				assertNull(this.template
						.queryForObject("SELECT last_name from person where person.first_name=?", new String[] {person.getFirstName()}, String.class));
			}
		}
	}

}
