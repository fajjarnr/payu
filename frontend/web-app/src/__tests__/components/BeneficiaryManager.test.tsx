import React from "react";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { vi, beforeEach } from "vitest";
import BeneficiaryManager from "@/components/account/BeneficiaryManager";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

const getBeneficiaries = vi.fn(async () => [
  {
    id: "ben_1",
    bankCode: "PAYU",
    accountNumber: "1001001002",
    accountName: "Audit Penerima",
    nickname: "Audit Penerima",
    status: "ACTIVE",
  },
]);
const createBeneficiary = vi.fn(async () => ({
  id: "ben_2",
  bankCode: "PAYU",
  accountNumber: "1234567890",
  accountName: "Penerima Baru",
  status: "ACTIVE",
}));

vi.mock("@/services/AccountService", () => ({
  default: {
    getBeneficiaries: (...args: unknown[]) => getBeneficiaries(...(args as [])),
    createBeneficiary: (...args: unknown[]) =>
      createBeneficiary(...(args as [])),
    deleteBeneficiary: async () => {},
  },
}));

describe("BeneficiaryManager", () => {
  beforeEach(() => {
    getBeneficiaries.mockClear();
    createBeneficiary.mockClear();
  });

  it("lists existing beneficiaries", async () => {
    renderWithIntl(<BeneficiaryManager accountId="acc_1" />);
    expect(await screen.findByText("Audit Penerima")).toBeInTheDocument();
    expect(getBeneficiaries).toHaveBeenCalledWith("acc_1");
  });

  it("creates a beneficiary through AccountService", async () => {
    const user = userEvent.setup();
    renderWithIntl(<BeneficiaryManager accountId="acc_1" />);

    await user.type(screen.getByTestId("beneficiary-bankCode"), "PAYU");
    await user.type(
      screen.getByTestId("beneficiary-accountNumber"),
      "1234567890",
    );
    await user.type(
      screen.getByTestId("beneficiary-nickname"),
      "Penerima Baru",
    );
    await user.click(screen.getByTestId("beneficiary-create"));

    await waitFor(() =>
      expect(createBeneficiary).toHaveBeenCalledWith("acc_1", {
        bankCode: "PAYU",
        accountNumber: "1234567890",
        nickname: "Penerima Baru",
      }),
    );
    // Form resets only after a successful call — the create button alone
    // must not silently discard user input.
    await waitFor(() =>
      expect(screen.getByTestId("beneficiary-accountNumber")).toHaveValue(""),
    );
  });

  it("refuses an invalid account number without calling the API", async () => {
    const user = userEvent.setup();
    renderWithIntl(<BeneficiaryManager accountId="acc_1" />);

    await user.type(screen.getByTestId("beneficiary-bankCode"), "PAYU");
    await user.type(screen.getByTestId("beneficiary-accountNumber"), "123");
    await user.click(screen.getByTestId("beneficiary-create"));

    expect(await screen.findByRole("alert")).toHaveTextContent("10-20 digit");
    expect(createBeneficiary).not.toHaveBeenCalled();
  });
});
