export default function TransferLoading() {
  return (
    <div
      className="min-h-screen animate-pulse"
      style={{ backgroundColor: "var(--ant-color-bg-layout)" }}
    >
      <div
        className="h-16 border-b"
        style={{
          borderColor: "var(--ant-color-border-secondary)",
          backgroundColor: "var(--ant-color-bg-container)",
        }}
      />

      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div
          className="h-8 w-40 rounded-xl mb-2"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />
        <div
          className="h-5 w-56 rounded-xl mb-8"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />

        <div
          className="rounded-2xl border p-6 space-y-6"
          style={{
            borderColor: "var(--ant-color-border-secondary)",
            backgroundColor: "var(--ant-color-bg-container)",
          }}
        >
          <div>
            <div
              className="h-4 w-24 rounded mb-2"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-12 w-full rounded-xl"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
          <div>
            <div
              className="h-4 w-32 rounded mb-2"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-12 w-full rounded-xl"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
          <div>
            <div
              className="h-4 w-20 rounded mb-2"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-12 w-full rounded-xl"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
          <div>
            <div
              className="h-4 w-28 rounded mb-2"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-12 w-full rounded-xl"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
          <div
            className="h-12 w-full rounded-full"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
        </div>

        <div className="mt-8">
          <div
            className="h-6 w-36 rounded-xl mb-4"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div className="flex gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex flex-col items-center gap-2">
                <div
                  className="w-14 h-14 rounded-full"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
                <div
                  className="h-3 w-12 rounded"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
