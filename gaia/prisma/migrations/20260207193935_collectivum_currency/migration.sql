-- CreateEnum
CREATE TYPE "MembershipState" AS ENUM ('INVITED', 'JOINED');

-- CreateEnum
CREATE TYPE "CurrencyType" AS ENUM ('COINS');

-- CreateTable
CREATE TABLE "alliances" (
    "id" TEXT NOT NULL,
    "name" VARCHAR(30) NOT NULL,
    "description" VARCHAR(200) NOT NULL,
    "motd" VARCHAR(300) NOT NULL,
    "ownedBy" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "alliances_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alliance_members" (
    "id" TEXT NOT NULL,
    "uuid" TEXT NOT NULL,
    "cachedName" TEXT NOT NULL,
    "membershipState" "MembershipState" NOT NULL DEFAULT 'INVITED',
    "addedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "addedBy" TEXT NOT NULL,
    "permissions" TEXT[],
    "allianceId" TEXT NOT NULL,

    CONSTRAINT "alliance_members_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "currency_submissions" (
    "id" TEXT NOT NULL,
    "submitterName" TEXT NOT NULL,
    "submitterUuid" TEXT NOT NULL,
    "submittedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "modVersion" TEXT NOT NULL,

    CONSTRAINT "currency_submissions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "currency_records" (
    "id" TEXT NOT NULL,
    "username" TEXT NOT NULL,
    "uuid" TEXT NOT NULL,
    "currencyType" "CurrencyType" NOT NULL,
    "amount" BIGINT NOT NULL,
    "submissionId" TEXT NOT NULL,

    CONSTRAINT "currency_records_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "currency_records_uuid_idx" ON "currency_records"("uuid");

-- CreateIndex
CREATE INDEX "currency_records_submissionId_idx" ON "currency_records"("submissionId");

-- AddForeignKey
ALTER TABLE "alliance_members" ADD CONSTRAINT "alliance_members_allianceId_fkey" FOREIGN KEY ("allianceId") REFERENCES "alliances"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "currency_records" ADD CONSTRAINT "currency_records_submissionId_fkey" FOREIGN KEY ("submissionId") REFERENCES "currency_submissions"("id") ON DELETE CASCADE ON UPDATE CASCADE;
