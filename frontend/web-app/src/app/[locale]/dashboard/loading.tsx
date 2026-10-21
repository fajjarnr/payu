export default function DashboardLoading() {
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

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div
          className="h-8 w-48 rounded-xl mb-2"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />
        <div
          className="h-5 w-64 rounded-xl mb-8"
          style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
        />

        <div
          className="p-6 rounded-2xl border mb-8"
          style={{
            borderColor: "var(--ant-color-border-secondary)",
            backgroundColor: "var(--ant-color-bg-container)",
          }}
        >
          <div
            className="h-4 w-24 rounded mb-3"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div
            className="h-10 w-56 rounded-xl mb-4"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div className="flex gap-3">
            <div
              className="h-10 w-28 rounded-full"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-10 w-28 rounded-full"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
            <div
              className="h-10 w-28 rounded-full"
              style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
            />
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="p-4 rounded-2xl border flex flex-col items-center gap-2"
              style={{
                borderColor: "var(--ant-color-border-secondary)",
                backgroundColor: "var(--ant-color-bg-container)",
              }}
            >
              <div
                className="w-12 h-12 rounded-full"
                style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
              />
              <div
                className="h-3 w-16 rounded"
                style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
              />
            </div>
          ))}
        </div>

        <div
          className="rounded-2xl border p-6"
          style={{
            borderColor: "var(--ant-color-border-secondary)",
            backgroundColor: "var(--ant-color-bg-container)",
          }}
        >
          <div
            className="h-6 w-40 rounded-xl mb-6"
            style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
          />
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <div
                  className="w-10 h-10 rounded-full"
                  style={{ backgroundColor: "var(--ant-color-fill-secondary)" }}
                />
                <div className="flex-1">
                  <div
                    className="h-4 w-32 rounded mb-1"
                    style={{
                      backgroundColor: "var(--ant-color-fill-secondary)",
                    }}
                  />
                  <div
                    className="h-3 w-24 rounded"
                    style={{
                      backgroundColor: "var(--ant-color-fill-secondary)",
                    }}
                  />
                </div>
                <div
                  className="h-4 w-20 rounded"
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
