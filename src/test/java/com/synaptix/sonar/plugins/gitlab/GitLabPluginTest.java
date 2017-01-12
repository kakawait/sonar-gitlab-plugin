/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2009-2016 Thibaud Leprêtre
 * thibaud.lepretre@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.synaptix.sonar.plugins.gitlab;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GitLabPluginTest {

    @Test
    public void uselessTest() {
        Map<String, Map<String, Set<Integer>>> a = new HashMap<>();
        HashMap<String, Set<Integer>> b = new HashMap<>();
        b.put(".gitlab-ci.yml", new HashSet<Integer>() {{
            add(-1);
            add(40);
            add(41);
            add(42);
            add(43);
            add(44);
            add(45);
            add(46);
        }});
        a.put("b6ea91d1e0ee44e8ece914052e2da9bbe3186b04", b);
        b = new HashMap<>();
        b.put("src/main/java/com/dassault_systemes/security/UpdatableJdbcClientDetailsServiceBuilder.java", new HashSet<Integer>() {{
            add(-1);
            add(64);
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
            add(6);
            add(7);
            add(21);
            add(22);
            add(23);
            add(24);
            add(25);
            add(57);
            add(26);
            add(58);
            add(27);
            add(59);
            add(28);
            add(60);
            add(61);
            add(62);
            add(63);
        }});
        b.put("src/main/java/com/dassault_systemes/security/AuthorizationServerConfiguration.java", new HashSet<Integer>() {{
            add(-1);
            add(32);
            add(33);
            add(34);
            add(35);
            add(36);
            add(11);
            add(12);
            add(13);
            add(14);
            add(15);
            add(16);
            add(23);
            add(24);
            add(25);
            add(26);
            add(27);
            add(28);
            add(29);
            add(30);
            add(31);
        }});
        a.put("54c6b819c0db4b66af4f145f8d99bbc7e14b63ee", b);
        b = new HashMap<>();
        b.put("src/main/java/com/dassault_systemes/security/UpdatableJdbcClientDetailsServiceBuilder.java", new HashSet<Integer>() {{
            add(-1);
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
            add(6);
            add(7);
            add(8);
            add(9);
            add(10);
            add(11);
            add(12);
            add(13);
            add(14);
            add(15);
            add(16);
            add(17);
            add(18);
            add(19);
            add(20);
            add(21);
            add(22);
            add(23);
            add(24);
            add(25);
            add(26);
            add(27);
            add(28);
            add(29);
            add(30);
            add(31);
            add(32);
            add(33);
            add(34);
            add(35);
            add(36);
            add(37);
            add(38);
            add(39);
            add(40);
            add(41);
            add(42);
            add(43);
            add(44);
            add(45);
            add(46);
            add(47);
            add(48);
            add(49);
            add(50);
            add(51);
            add(52);
            add(53);
            add(54);
            add(55);
            add(56);
            add(57);
            add(58);
            add(59);
            add(60);
            add(61);
        }});
        b.put("src/main/java/com/dassault_systemes/security/AuthorizationServerConfiguration.java", new HashSet<Integer>() {{
            add(-1);
            add(10);
            add(11);
            add(12);
            add(13);
            add(14);
            add(15);
            add(16);
            add(17);
            add(39);
            add(40);
            add(41);
            add(42);
            add(43);
            add(44);
            add(45);
            add(46);
            add(47);
            add(48);
            add(49);
            add(50);
            add(51);
            add(52);
            add(53);
            add(54);
            add(55);
            add(56);
            add(57);
            add(58);
            add(60);
            add(61);
            add(62);
            add(63);
            add(64);
            add(65);
            add(66);
            add(67);
            add(68);
            add(69);
            add(70);
            add(71);
            add(72);
            add(73);
            add(74);
            add(75);
            add(76);
            add(77);
            add(78);
            add(79);
            add(80);
            add(81);
            add(82);
            add(83);
        }});
        a.put("c9643481d9428163a1f3058037be495968ced3dd", b);

        Optional<String> first = a
                .entrySet()
                .stream()
                .filter(e -> e.getValue().entrySet().stream()
                              .anyMatch(v -> v.getKey().equals("src/main/java/com/dassault_systemes/security/UpdatableJdbcClientDetailsServiceBuilder.java") && v.getValue().contains(59)))
                .map(Map.Entry::getKey)
                .findFirst();

        Plugin.Context context = new Plugin.Context(SonarRuntimeImpl.forSonarLint(Version.parse("6.2")));
        new GitLabPlugin().define(context);
        assertThat(context.getExtensions().size()).isGreaterThan(1);
    }

}
