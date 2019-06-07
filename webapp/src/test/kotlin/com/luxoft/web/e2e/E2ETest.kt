/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.luxoft.web.e2e

import com.luxoft.poc.supplychain.data.PackageInfo
import com.luxoft.poc.supplychain.data.PackageState
import com.luxoft.web.clients.ManufactureClient
import com.luxoft.web.clients.TreatmentCenterClient
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.lang.Thread.sleep
import javax.annotation.PostConstruct

@RunWith(SpringRunner::class)
@ActiveProfiles(profiles = [])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Ignore
class E2ETest {
    @Autowired
    lateinit var restTemplateBuilder: RestTemplateBuilder

    lateinit var manufactureClient: ManufactureClient
    lateinit var treatmentCenterClient: TreatmentCenterClient

    @PostConstruct
    fun initialize() {
        val mfRestTemplate = restTemplateBuilder.rootUri("http://localhost:8081").build()
        val tcRestTemplate = restTemplateBuilder.rootUri("http://localhost:8082").build()

        manufactureClient = ManufactureClient(mfRestTemplate)
        treatmentCenterClient = TreatmentCenterClient(tcRestTemplate)
    }

    @Test
    fun mainFlow() {
        `treatment center can issue new package`()
        `manufacturer can process issued package`()
        `treatment center can receive package`()
        `treatment center can give package`()
        `treatment center can observe packages`()
    }

    val syncUpDelay = 5000L

    fun `treatment center can issue new package`() {
        val packagesBefore = treatmentCenterClient.getPackages()
        val packagesCountBefore = packagesBefore.filter { packageHasStatus(it, PackageState.ISSUED) }.size

        //TODO: Take credentials required for initFlow
        val invite = treatmentCenterClient.getInvite()
        treatmentCenterClient.initFlow("Treatment London GB", invite)

        sleep(syncUpDelay)
        val packagesAfter = treatmentCenterClient.getPackages()
        val packagesCountAfter = packagesAfter.filter { packageHasStatus(it, PackageState.ISSUED) }.size

        assert(packagesCountBefore < packagesCountAfter)
    }

    fun `treatment center can receive package`() {
        val packagesBefore = treatmentCenterClient.getPackages()
        assert(packagesBefore.isNotEmpty())

        val packagesCountBefore = packagesBefore.filter { packageHasStatus(it, PackageState.DELIVERED) }.size

        val readyToReceivePackage = packagesBefore.find { packageHasStatus(it, PackageState.PROCESSED) }!!
//        assertPackageValid(readyToReceivePackage)

        treatmentCenterClient.receivePackage(readyToReceivePackage.serial)

        sleep(syncUpDelay)
        val packagesAfter = treatmentCenterClient.getPackages()
        assert(packagesAfter.isNotEmpty())

        val packagesCountAfter = packagesAfter.filter { packageHasStatus(it, PackageState.DELIVERED) }.size
        assert(packagesCountBefore < packagesCountAfter)
    }

    fun `treatment center can give package`() {
        val packagesBefore = treatmentCenterClient.getPackages()
        assert(packagesBefore.isNotEmpty())

        val packagesCountBefore = packagesBefore.filter { packageHasStatus(it, PackageState.COLLECTED) }.size

        val readyToGivePackage = packagesBefore.find { packageHasStatus(it, PackageState.DELIVERED) }!!
//        assertPackageValid(readyToCollectPackage)

        treatmentCenterClient.collectPackage(readyToGivePackage.serial, treatmentCenterClient.getInvite())

        sleep(syncUpDelay)
        val packagesAfter = treatmentCenterClient.getPackages()
        assert(packagesAfter.isNotEmpty())

        val packagesCountAfter = packagesAfter.filter { packageHasStatus(it, PackageState.COLLECTED) }.size
        assert(packagesCountBefore < packagesCountAfter)
    }

    fun `manufacturer can process issued package`() {
        val packagesBefore = manufactureClient.getPackages()
        assert(packagesBefore.isNotEmpty())

        val packagesCountBefore = packagesBefore.filter { packageHasStatus(it, PackageState.PROCESSED) }.size

        val packageToProcess = packagesBefore.find { packageHasStatus(it, PackageState.ISSUED) }!!
//        assertPackageValid(packageToProcess)

        manufactureClient.processPackage(packageToProcess.serial)

        sleep(syncUpDelay)
        val packagesAfter = manufactureClient.getPackages()
        assert(packagesAfter.isNotEmpty())

        val packagesCountAfter = packagesAfter.filter { packageHasStatus(it, PackageState.PROCESSED) }.size
        assert(packagesCountBefore < packagesCountAfter)
    }

    fun `treatment center can observe packages`() {
        val packages = treatmentCenterClient.getPackages()
        assert(packages.isNotEmpty())
    }

    private fun packageHasStatus(pack: PackageInfo, status: PackageState): Boolean {
        return pack.state == status
    }

//    private fun assertPackageValid(pack: PackageInfo) {
//        assert(pack.serial.isNotEmpty())
//        assert(pack.status in 0..5)
//        assert(pack.manufacturer.isNotEmpty())
//        assert(pack.patientDid.isNotEmpty())
//        assert(pack.patientDiagnosis.isNotEmpty())
//        assert(pack.medicineName.isNotEmpty())
//        assert(pack.medicineDescription.isNotEmpty())
//        assert(pack.treatmentCenterName.isNotEmpty())
//        assert(pack.treatmentCenterAddress.isNotEmpty())
//
//        when (pack.status) {
//            1 -> {
//                assertWaypointPresence(pack.issuedAt, pack.issuedBy)
//            }
//            2 -> {
//                assertWaypointPresence(pack.issuedAt, pack.issuedBy)
//                assertWaypointPresence(pack.processedAt, pack.processedBy)
//            }
//            3 -> {
//                assertWaypointPresence(pack.issuedAt, pack.issuedBy)
//                assertWaypointPresence(pack.processedAt, pack.processedBy)
//                assertWaypointPresence(pack.deliveredAt, pack.deliveredBy)
//            }
//            4 -> {
//                assertWaypointPresence(pack.issuedAt, pack.issuedBy)
//                assertWaypointPresence(pack.processedAt, pack.processedBy)
//                assertWaypointPresence(pack.deliveredAt, pack.deliveredBy)
//                assert(pack.qp)
//            }
//            5 -> {
//                assertWaypointPresence(pack.issuedAt, pack.issuedBy)
//                assertWaypointPresence(pack.processedAt, pack.processedBy)
//                assertWaypointPresence(pack.deliveredAt, pack.deliveredBy)
//                assert(pack.qp)
//                assertWaypointPresence(pack.collectedAt, pack.collectedBy)
//            }
//        }
//    }

    private fun assertWaypointPresence(at: Long, by: String) {
        assert(at > 0)
        assert(by.isNotEmpty())
    }
}
